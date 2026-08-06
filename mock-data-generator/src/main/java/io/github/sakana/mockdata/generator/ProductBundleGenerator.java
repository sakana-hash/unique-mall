package io.github.sakana.mockdata.generator;

import io.github.sakana.mockdata.config.MockDataProperties;
import io.github.sakana.product.pojo.entity.Product;
import io.github.sakana.product.pojo.entity.ProductDetail;
import io.github.sakana.product.pojo.entity.ProductImage;
import io.github.sakana.product.pojo.entity.ProductSKU;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;

import static io.github.sakana.mockdata.generator.MockContent.*;

/**
 * 生成一个“商品聚合”：product + SKU 列表 + product_detail + product_image 列表。
 * 线程安全（无可变共享状态），可在多线程中并发调用。
 */
public class ProductBundleGenerator {

    /**
     * 一个商品及其关联数据
     */
    public record Bundle(Product product, List<ProductSKU> skus, ProductDetail detail, List<ProductImage> images) {
    }

    private static final long DAY_SECONDS = 24L * 3600;

    private final MockDataProperties props;
    private final long[] leafCategoryIds;
    private final LocalDateTime now;

    public ProductBundleGenerator(MockDataProperties props, List<Long> leafCategoryIds) {
        this.props = props;
        this.leafCategoryIds = leafCategoryIds.stream().mapToLong(Long::longValue).toArray();
        this.now = LocalDateTime.now();
    }

    /**
     * 生成下一个商品聚合，所有主键均取自传入的雪花ID生成器
     */
    public Bundle next(LongSupplier idGen) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        long productId = idGen.getAsLong();
        long categoryId = leafCategoryIds[rnd.nextInt(leafCategoryIds.length)];

        LocalDateTime created = now.minusSeconds(rnd.nextLong(props.getCreatedWithinDays() * DAY_SECONDS));
        LocalDateTime updated = clamp(created.plusSeconds(rnd.nextLong(0, 30 * DAY_SECONDS)));

        String brand = pick(BRANDS, rnd);
        String noun = pick(PRODUCT_NOUNS, rnd);
        String edition = pick(EDITIONS, rnd);
        String name = brand + " " + noun + " " + edition;

        Product product = new Product();
        product.setId(productId);
        product.setCategoryId(categoryId);
        product.setName(name);
        product.setSubtitle(buildSubtitle(rnd));
        product.setBrand(brand);
        product.setStatus(rnd.nextInt(10) < 9 ? 1 : 0);
        product.setCreatedTime(created);
        product.setUpdatedTime(updated);

        return new Bundle(product, buildSkus(idGen, rnd, productId, created, product.getStatus()),
                props.isDetailEnabled() ? buildDetail(idGen, productId, name, brand, created, updated) : null,
                buildImages(idGen, rnd, productId, created));
    }

    private List<ProductSKU> buildSkus(LongSupplier idGen, ThreadLocalRandom rnd,
                                       long productId, LocalDateTime productCreated, Integer productStatus) {
        int count = rnd.nextInt(props.getSkuPerProductMin(), props.getSkuPerProductMax() + 1);
        // 基准价 9.9 ~ 19999 元（单位：分），平方分布偏向低价
        long basePrice = 990L + (long) (Math.pow(rnd.nextDouble(), 2.0) * 1_998_911);

        List<ProductSKU> skus = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            LocalDateTime skuCreated = clamp(productCreated.plusSeconds(rnd.nextLong(0, 3 * DAY_SECONDS)));
            ProductSKU sku = new ProductSKU();
            sku.setId(idGen.getAsLong());
            sku.setProductId(productId);
            // product_id 全局唯一 => sku_code 天然唯一
            sku.setSkuCode("SKU-" + productId + "-" + twoDigits(i + 1));
            // 每个规格比上一个贵 8%
            sku.setPrice(basePrice * (100L + 8L * i) / 100);
            sku.setStatus(rnd.nextInt(10) < 9 ? 1 : 0);
            // product_status 与商品上下架状态保持一致，供价格排序查询过滤使用
            sku.setProductStatus(productStatus);
            sku.setCreatedTime(skuCreated);
            sku.setUpdatedTime(clamp(skuCreated.plusSeconds(rnd.nextLong(0, 15 * DAY_SECONDS))));
            skus.add(sku);
        }
        return skus;
    }

    private ProductDetail buildDetail(LongSupplier idGen, long productId, String name, String brand,
                                      LocalDateTime created, LocalDateTime updated) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        String highlights = String.join("；", pickDistinct(HIGHLIGHTS, 3, rnd));

        ProductDetail detail = new ProductDetail();
        detail.setId(idGen.getAsLong());
        detail.setProductId(productId);
        detail.setContent("<p><strong>" + name + "</strong></p>"
                + "<p>品牌：" + brand + "</p>"
                + "<p>商品亮点：" + highlights + "</p>"
                + "<p>" + DETAIL_SUFFIX_TEXT + "</p>");
        detail.setCreatedTime(created);
        detail.setUpdatedTime(updated);
        return detail;
    }

    private List<ProductImage> buildImages(LongSupplier idGen, ThreadLocalRandom rnd,
                                           long productId, LocalDateTime created) {
        int count = rnd.nextInt(props.getImagePerProductMin(), props.getImagePerProductMax() + 1);
        List<ProductImage> images = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ProductImage image = new ProductImage();
            image.setId(idGen.getAsLong());
            image.setProductId(productId);
            image.setUrl(i == 0
                    ? "https://picsum.photos/seed/p" + productId + "m/800/800"
                    : "https://picsum.photos/seed/p" + productId + "d" + i + "/800/800");
            image.setType(i == 0 ? 1 : 2);
            image.setSort(i);
            image.setCreatedTime(created);
            images.add(image);
        }
        return images;
    }

    private String buildSubtitle(ThreadLocalRandom rnd) {
        return String.join(" · ", pickDistinct(SUBTITLE_PARTS, rnd.nextInt(2, 4), rnd));
    }

    private LocalDateTime clamp(LocalDateTime time) {
        return time.isAfter(now) ? now : time;
    }

    private static String pick(String[] pool, ThreadLocalRandom rnd) {
        return pool[rnd.nextInt(pool.length)];
    }

    /**
     * 从词池中不重复地随机取 count 个
     */
    private static String[] pickDistinct(String[] pool, int count, ThreadLocalRandom rnd) {
        String[] result = new String[count];
        for (int i = 0; i < count; i++) {
            String candidate;
            boolean duplicate;
            do {
                candidate = pool[rnd.nextInt(pool.length)];
                duplicate = false;
                for (int j = 0; j < i; j++) {
                    if (result[j].equals(candidate)) {
                        duplicate = true;
                        break;
                    }
                }
            } while (duplicate);
            result[i] = candidate;
        }
        return result;
    }

    private static String twoDigits(int n) {
        return n < 10 ? "0" + n : String.valueOf(n);
    }
}
