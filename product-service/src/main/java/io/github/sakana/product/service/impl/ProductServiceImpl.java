package io.github.sakana.product.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.sakana.product.mapper.ProductDetailMapper;
import io.github.sakana.product.mapper.ProductImageMapper;
import io.github.sakana.product.mapper.ProductMapper;
import io.github.sakana.product.mapper.ProductSKUMapper;
import io.github.sakana.product.pojo.ProductPageQuery;
import io.github.sakana.product.pojo.dto.ProductPageDTO;
import io.github.sakana.product.pojo.entity.*;
import io.github.sakana.product.pojo.vo.PageVO;
import io.github.sakana.product.pojo.vo.ProductPageVO;
import io.github.sakana.product.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private ProductSKUMapper skuMapper;
    @Autowired
    private ProductDetailMapper detailMapper;
    @Autowired
    private ProductImageMapper imageMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    private static final String PRODUCT_KEY_PREFIX = "product-service:product:";
    private static final long PRODUCT_CACHE_EXPIRE_TIME = 600;
    private static final String PAGE_RESULT_KEY_PREFIX = "product-service:page:";
    private static final long PAGE_RESULT_CACHE_EXPIRE_TIME = 60;


    @Override
    public PageVO<ProductPageVO> page(ProductPageDTO pageDTO) {
        Integer page = pageDTO.getPage();
        Integer size = pageDTO.getSize();
        Long categoryId = pageDTO.getCategoryId();
        if (page == null || page < 1) {
            page = 1;
        }
        if (size == null || size < 1) {
            size = 1;
        } else if (size > 100) {
            size = 100;
        }
        if (categoryId != null && categoryId <= 0) {
            throw new RuntimeException(String.format(
                    "无效的categoryId: %d", categoryId
            ));
        }
        String pageResultKey = PAGE_RESULT_KEY_PREFIX + page + ":size:" + size
                + ":sort:" + pageDTO.getSort();
        if (categoryId != null) {
            pageResultKey += ":category:" + categoryId;
        }

        PageResult pageResult = null;
        try {
            String json = redisTemplate.opsForValue().get(pageResultKey);
            if (json != null) {
                pageResult = mapper.readValue(json, PageResult.class);
            }
        } catch (JsonProcessingException e) {
            // 缓存数据反序列化失败视为未命中，回源数据库
            log.warn("分页结果缓存反序列化失败", e);
        } catch (RuntimeException e) {
            // Redis 不可用时降级为直接查库
            log.warn("读取分页结果缓存失败，降级为数据库查询", e);
        }

        if (pageResult == null || pageResult.getIds() == null || pageResult.getTotal() == null) {
            ProductPageQuery query = new ProductPageQuery();
            query.setOffset((page - 1) * size);
            query.setSize(size);
            query.setCategoryId(pageDTO.getCategoryId());
            query.setSort(pageDTO.getSort());

            pageResult = new PageResult();
            pageResult.setIds(productMapper.selectPage(query));
            pageResult.setTotal(productMapper.count(query));
            try {
                redisTemplate.opsForValue().set(pageResultKey,
                        mapper.writeValueAsString(pageResult),
                        PAGE_RESULT_CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
            } catch (JsonProcessingException | RuntimeException e) {
                // 序列化或写入失败时仅跳过缓存，不影响本次返回
                log.warn("分页结果写入缓存失败", e);
            }
        }


        List<Product> products = batchGetDetails(pageResult.getIds());
        List<ProductPageVO> productPageVOS = products.stream()
                .map(Product::toPageVO)
                .collect(Collectors.toList());
        PageVO<ProductPageVO> pageVO = new PageVO<>();
        pageVO.setItems(productPageVOS);
        pageVO.setTotal(pageResult.getTotal());
        pageVO.setPage(page);
        pageVO.setSize(size);
        pageVO.setPages((pageResult.getTotal() + size - 1) / size);
        return pageVO;
    }

    /**
     * 根据 id 批量获取商品明细：优先从 Redis 缓存读取，未命中的 id 再查询数据库并重建缓存，
     * 最终按输入 ids 的顺序合并返回；Redis 异常时降级为纯数据库查询。
     *
     * @param ids 商品 id 列表
     * @return 商品明细列表（与入参 ids 顺序一致）
     */
    private List<Product> batchGetDetails(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }

        // 1. 构建 redis key 并尝试批量获取 products
        List<String> keys = ids.stream()
                .map(id -> PRODUCT_KEY_PREFIX + id)
                .collect(Collectors.toList());

        Map<Long, Product> cachedMap = new HashMap<>();
        List<Long> missIds = new ArrayList<>();
        try {
            List<String> cachedJsonList = redisTemplate.opsForValue().multiGet(keys);
            for (int i = 0; i < ids.size(); i++) {
                Long id = ids.get(i);
                String cachedJson = cachedJsonList == null ? null : cachedJsonList.get(i);
                if (cachedJson == null) {
                    missIds.add(id);
                    continue;
                }
                try {
                    Product product = mapper.readValue(cachedJson, Product.class);
                    if (product.getImages() == null) {
                        product.setImages(Collections.emptyList());
                    }
                    if (product.getSkus() == null) {
                        product.setSkus(Collections.emptyList());
                    }
                    cachedMap.put(id, product);
                } catch (JsonProcessingException e) {
                    // 缓存数据反序列化失败视为未命中，回源数据库
                    log.warn("商品缓存反序列化失败, id={}", id, e);
                    missIds.add(id);
                }
            }
        } catch (RuntimeException e) {
            // Redis 不可用时降级为全量回源数据库
            log.warn("读取商品缓存失败，降级为数据库查询", e);
            missIds = new ArrayList<>(ids);
            cachedMap.clear();
        }

        // 2. 未命中的 id 去数据库查询并组装
        if (!missIds.isEmpty()) {
            List<Product> dbProducts = queryAndAssemble(missIds);

            // 3. 将未命中的 products 写回 redis 缓存
            for (Product product : dbProducts) {
                cachedMap.put(product.getId(), product);
                try {
                    redisTemplate.opsForValue().set(
                            PRODUCT_KEY_PREFIX + product.getId(),
                            mapper.writeValueAsString(product),
                            PRODUCT_CACHE_EXPIRE_TIME,
                            TimeUnit.SECONDS);
                } catch (JsonProcessingException | RuntimeException e) {
                    // 序列化或写入失败时仅跳过缓存，不影响本次返回
                    log.warn("商品写入缓存失败, id={}", product.getId(), e);
                }
            }
        }

        // 4. 合并缓存与数据库结果，并按输入 ids 的顺序返回
        List<Product> result = new ArrayList<>();
        for (Long id : ids) {
            Product product = cachedMap.get(id);
            if (product != null) {
                result.add(product);
            }
        }
        return result;
    }

    /**
     * 从数据库批量查询商品基础信息、图片、SKU、详情并组装为商品明细
     */
    private List<Product> queryAndAssemble(List<Long> ids) {
        List<Product> products = productMapper.selectByIds(ids);
        if (products.isEmpty()) {
            return products;
        }
        List<ProductImage> productImages = imageMapper.selectByProductIds(ids);
        List<ProductSKU> productSKUS = skuMapper.selectByProductIds(ids);
        List<ProductDetail> productDetails = detailMapper.selectByProductIds(ids);

        Map<Long, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));
        Map<Long, List<ProductImage>> imageMap = productImages.stream()
                .collect(Collectors.groupingBy(ProductImage::getProductId));
        Map<Long, List<ProductSKU>> skuMap = productSKUS.stream()
                .collect(Collectors.groupingBy(ProductSKU::getProductId));
        Map<Long, ProductDetail> detailMap = productDetails.stream()
                .collect(Collectors.toMap(
                        ProductDetail::getProductId,
                        Function.identity()
                ));

        List<Product> result = new ArrayList<>();
        for (Long id : ids) {
            Product product = productMap.get(id);
            if (product != null) {
                product.setImages(imageMap.getOrDefault(id, Collections.emptyList()));
                product.setSkus(skuMap.getOrDefault(id, Collections.emptyList()));
                product.setContent(detailMap.get(id) != null
                        ? detailMap.get(id).getContent()
                        : null);
                result.add(product);
            }
        }
        return result;
    }

}
