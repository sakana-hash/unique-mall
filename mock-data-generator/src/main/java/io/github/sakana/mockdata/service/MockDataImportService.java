package io.github.sakana.mockdata.service;

import io.github.sakana.mockdata.config.MockDataProperties;
import io.github.sakana.mockdata.generator.MockContent;
import io.github.sakana.mockdata.generator.ProductBundleGenerator;
import io.github.sakana.mockdata.generator.ProductBundleGenerator.Bundle;
import io.github.sakana.mockdata.mapper.MockProductCategoryMapper;
import io.github.sakana.mockdata.mapper.MockProductDetailMapper;
import io.github.sakana.mockdata.mapper.MockProductImageMapper;
import io.github.sakana.mockdata.mapper.MockProductMapper;
import io.github.sakana.mockdata.mapper.MockProductSkuMapper;
import io.github.sakana.mockdata.pojo.ProductCategory;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.entity.ProductDetail;
import io.github.sakana.product.pojo.entity.ProductImage;
import io.github.sakana.product.pojo.entity.ProductSKU;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.LongAdder;

/**
 * Mock 数据导入编排：
 * 1. （可选）TRUNCATE 目标表
 * 2. 生成并批量插入商品分类
 * 3. 多线程生成商品聚合（product + sku + detail + image），
 *    每攒满 mock.batch-size 行执行一次多行 VALUES 批量 INSERT
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MockDataImportService {

    private final MockDataProperties props;
    private final SnowflakeIdGenerator idGenerator;
    private final MockProductCategoryMapper categoryMapper;
    private final MockProductMapper productMapper;
    private final MockProductSkuMapper skuMapper;
    private final MockProductDetailMapper detailMapper;
    private final MockProductImageMapper imageMapper;

    private final LongAdder productDone = new LongAdder();
    private final LongAdder skuDone = new LongAdder();
    private final LongAdder detailDone = new LongAdder();
    private final LongAdder imageDone = new LongAdder();

    public void run() {
        props.validate();
        log.info("========== Mock 数据批量插入开始 ==========");
        log.info("配置: productCount={}, batchSize={}, threads={}, sku/product=[{},{}], image/product=[{},{}], "
                        + "detailEnabled={}, truncateFirst={}, dryRun={}",
                props.getProductCount(), props.getBatchSize(), props.getThreads(),
                props.getSkuPerProductMin(), props.getSkuPerProductMax(),
                props.getImagePerProductMin(), props.getImagePerProductMax(),
                props.isDetailEnabled(), props.isTruncateFirst(), props.isDryRun());

        long startNanos = System.nanoTime();

        if (props.isTruncateFirst() && !props.isDryRun()) {
            truncateTables();
        }

        List<Long> leafCategoryIds = importCategories();
        ProductBundleGenerator generator = new ProductBundleGenerator(props, leafCategoryIds);

        ExecutorService pool = Executors.newFixedThreadPool(props.getThreads(), runnable -> {
            Thread thread = new Thread(runnable, "mock-insert-" + THREAD_SEQ.incrementAndGet());
            thread.setDaemon(true);
            return thread;
        });
        ScheduledExecutorService reporter = startReporter();

        try {
            long total = props.getProductCount();
            int slices = props.getThreads() * 4;
            long sliceSize = Math.max(props.getBatchSize(), (total + slices - 1) / slices);

            List<Future<?>> futures = new ArrayList<>();
            for (long from = 0; from < total; from += sliceSize) {
                long sliceFrom = from;
                long sliceTo = Math.min(total, from + sliceSize);
                futures.add(pool.submit(() -> importSlice(generator, sliceFrom, sliceTo)));
            }
            for (Future<?> future : futures) {
                future.get();
            }
        } catch (Exception e) {
            log.error("Mock 数据插入失败，已插入 product={} sku={} detail={} image={}",
                    productDone.sum(), skuDone.sum(), detailDone.sum(), imageDone.sum(), e);
            throw new IllegalStateException("Mock 数据插入失败: " + e.getMessage(), e);
        } finally {
            reporter.shutdownNow();
            pool.shutdownNow();
        }

        double elapsedSeconds = (System.nanoTime() - startNanos) / 1e9;
        long totalRows = productDone.sum() + skuDone.sum() + detailDone.sum() + imageDone.sum();
        log.info("========== Mock 数据批量插入完成 ==========");
        log.info("product={}, sku={}, detail={}, image={}, 分类={}（一级）x {}（二级）",
                productDone.sum(), skuDone.sum(), detailDone.sum(), imageDone.sum(),
                Math.min(props.getCategoryLevel1Count(), MockContent.LEVEL1_CATEGORY_NAMES.length),
                props.getCategoryLevel2PerLevel1());
        log.info("总耗时 {}s, 总行数 {}, 平均速度 {} rows/s{}",
                String.format("%.1f", elapsedSeconds), totalRows,
                (long) (totalRows / elapsedSeconds), props.isDryRun() ? "（dry-run 未写库）" : "");
    }

    private void truncateTables() {
        log.info("TRUNCATE 目标表 ...");
        imageMapper.truncate();
        detailMapper.truncate();
        skuMapper.truncate();
        productMapper.truncate();
        categoryMapper.truncate();
    }

    /**
     * 生成两级分类树并批量插入，返回二级（叶子）分类ID列表
     */
    private List<Long> importCategories() {
        LocalDateTime now = LocalDateTime.now();
        int level1Count = Math.min(props.getCategoryLevel1Count(), MockContent.LEVEL1_CATEGORY_NAMES.length);
        int level2Count = props.getCategoryLevel2PerLevel1();

        List<ProductCategory> all = new ArrayList<>(level1Count * (level2Count + 1));
        List<Long> leafIds = new ArrayList<>(level1Count * level2Count);

        for (int i = 0; i < level1Count; i++) {
            ProductCategory level1 = new ProductCategory();
            level1.setId(idGenerator.nextId());
            level1.setParentId(0L);
            level1.setName(MockContent.LEVEL1_CATEGORY_NAMES[i]);
            level1.setLevel(1);
            level1.setStatus(1);
            level1.setCreatedTime(now);
            level1.setUpdatedTime(now);
            all.add(level1);

            for (int j = 0; j < level2Count; j++) {
                ProductCategory level2 = new ProductCategory();
                level2.setId(idGenerator.nextId());
                level2.setParentId(level1.getId());
                level2.setName(level1.getName() + "·" + MockContent.LEVEL2_CATEGORY_SUFFIXES[j]);
                level2.setLevel(2);
                level2.setStatus(1);
                level2.setCreatedTime(now);
                level2.setUpdatedTime(now);
                all.add(level2);
                leafIds.add(level2.getId());
            }
        }

        if (!props.isDryRun()) {
            for (List<ProductCategory> chunk : partition(all, props.getBatchSize())) {
                categoryMapper.batchInsert(chunk);
            }
        }
        log.info("分类生成完成: 一级 {} 个, 二级 {} 个", level1Count, leafIds.size());
        return leafIds;
    }

    /**
     * 处理一个商品分片 [from, to)：生成数据并按 batch size 攒批插入
     */
    private void importSlice(ProductBundleGenerator generator, long from, long to) {
        int batchSize = props.getBatchSize();
        List<Product> productBuf = new ArrayList<>(batchSize);
        List<ProductSKU> skuBuf = new ArrayList<>(batchSize * 2);
        List<ProductDetail> detailBuf = new ArrayList<>(batchSize);
        List<ProductImage> imageBuf = new ArrayList<>(batchSize * 2);

        try {
            for (long i = from; i < to; i++) {
                Bundle bundle = generator.next(idGenerator::nextId);
                productBuf.add(bundle.product());
                skuBuf.addAll(bundle.skus());
                if (bundle.detail() != null) {
                    detailBuf.add(bundle.detail());
                }
                imageBuf.addAll(bundle.images());

                if (productBuf.size() >= batchSize) {
                    flushProducts(productBuf);
                }
                if (skuBuf.size() >= batchSize) {
                    flushSkus(skuBuf);
                }
                if (detailBuf.size() >= batchSize) {
                    flushDetails(detailBuf);
                }
                if (imageBuf.size() >= batchSize) {
                    flushImages(imageBuf);
                }
            }
            flushProducts(productBuf);
            flushSkus(skuBuf);
            flushDetails(detailBuf);
            flushImages(imageBuf);
        } catch (Exception e) {
            log.error("分片 [{}, {}) 插入异常", from, to, e);
            throw e;
        }
    }

    private void flushProducts(List<Product> buf) {
        if (buf.isEmpty()) {
            return;
        }
        if (!props.isDryRun()) {
            productMapper.batchInsert(buf);
        }
        productDone.add(buf.size());
        buf.clear();
    }

    private void flushSkus(List<ProductSKU> buf) {
        if (buf.isEmpty()) {
            return;
        }
        if (!props.isDryRun()) {
            skuMapper.batchInsert(buf);
        }
        skuDone.add(buf.size());
        buf.clear();
    }

    private void flushDetails(List<ProductDetail> buf) {
        if (buf.isEmpty()) {
            return;
        }
        if (!props.isDryRun()) {
            detailMapper.batchInsert(buf);
        }
        detailDone.add(buf.size());
        buf.clear();
    }

    private void flushImages(List<ProductImage> buf) {
        if (buf.isEmpty()) {
            return;
        }
        if (!props.isDryRun()) {
            imageMapper.batchInsert(buf);
        }
        imageDone.add(buf.size());
        buf.clear();
    }

    /**
     * 每 5 秒打印一次进度和实时速率
     */
    private ScheduledExecutorService startReporter() {
        ScheduledExecutorService reporter = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "mock-progress");
            thread.setDaemon(true);
            return thread;
        });
        final long total = props.getProductCount();
        final long[] last = new long[4];
        final long[] lastNanos = {System.nanoTime()};

        reporter.scheduleAtFixedRate(() -> {
            long p = productDone.sum();
            long s = skuDone.sum();
            long d = detailDone.sum();
            long im = imageDone.sum();
            long nowNanos = System.nanoTime();
            double intervalSeconds = (nowNanos - lastNanos[0]) / 1e9;
            long intervalRows = (p - last[0]) + (s - last[1]) + (d - last[2]) + (im - last[3]);
            last[0] = p;
            last[1] = s;
            last[2] = d;
            last[3] = im;
            lastNanos[0] = nowNanos;
            log.info("进度: product {}/{} ({}%) | sku {} | detail {} | image {} | 速率 ~{} rows/s",
                    p, total, String.format("%.1f", p * 100.0 / total), s, d, im,
                    (long) (intervalRows / intervalSeconds));
        }, 5, 5, TimeUnit.SECONDS);
        return reporter;
    }

    private static final AtomicInteger THREAD_SEQ = new AtomicInteger();

    private static <T> List<List<T>> partition(List<T> list, int size) {
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(list.subList(i, Math.min(list.size(), i + size)));
        }
        return result;
    }
}
