package io.github.sakana.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.sakana.product.enumeration.PageSort;
import io.github.sakana.product.pojo.entity.PageResult;
import io.github.sakana.product.pojo.entity.Product;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.github.sakana.product.constant.ProductConstants.PAGE_RESULT_CACHE_PHYSICAL_TTL_SECONDS;
import static io.github.sakana.product.constant.ProductConstants.PAGE_RESULT_CACHE_TTL_SECONDS;
import static io.github.sakana.product.constant.ProductConstants.PRODUCT_CACHE_TTL_SECONDS;
import static io.github.sakana.product.constant.ProductConstants.MIN_VALID_ID;

@Component
@Slf4j
public class CacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final String PRODUCT_KEY_PREFIX = "product-service:product";
    private static final long PRODUCT_CACHE_EXPIRE_TIME = PRODUCT_CACHE_TTL_SECONDS;

    private static final String PAGE_RESULT_KEY_PREFIX = "product-service:page";
    private static final long PAGE_RESULT_CACHE_LOGICAL_EXPIRE_TIME =
            PAGE_RESULT_CACHE_TTL_SECONDS;
    private static final long PAGE_RESULT_CACHE_PHYSICAL_EXPIRE_TIME =
            PAGE_RESULT_CACHE_PHYSICAL_TTL_SECONDS;


    public String buildPageResultKey(Integer page, Integer size, Long categoryId, PageSort sort) {
        if (sort == null) {
            sort = PageSort.DEFAULT;
        }

        String key = PAGE_RESULT_KEY_PREFIX + ":" + page + ":size:" + size + ":sort:" + sort;
        if (categoryId != null) {
            return key + ":category:" + categoryId;
        }

        return key;
    }

    public PageResult getPageResult(String key) {
        String json;
        try {
            json = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("读取分页结果缓存失败，降级为数据库查询", e);
            return null;
        }
        if (json == null) {
            return null;
        }

        PageResult pageResult;
        try {
            pageResult = mapper.readValue(json, PageResult.class);
        } catch (JsonProcessingException e) {
            log.warn("分类结果缓存反序列化失败", e);
            return null;
        }

        if (!isCompletePageResult(pageResult)) {
            log.warn("分页结果缓存数据不完整, key={}", key);
            return null;
        }
        return pageResult;
    }

    public void setPageResult(String key, PageResult result) {
        result.setExpireTime(LocalDateTime.now().plusSeconds(
                PAGE_RESULT_CACHE_LOGICAL_EXPIRE_TIME
        ));

        try {
            redisTemplate.opsForValue().set(key,
                    mapper.writeValueAsString(result),
                    PAGE_RESULT_CACHE_PHYSICAL_EXPIRE_TIME,
                    TimeUnit.SECONDS);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("分页结果缓存写入失败，跳过缓存", e);
        }
    }

    private static boolean isCompletePageResult(PageResult result) {
        return result != null
                && result.getIds() != null
                && result.getIds().stream()
                        .allMatch(id -> id != null && id >= MIN_VALID_ID)
                && result.getTotal() != null
                && result.getTotal() >= 0
                && result.getExpireTime() != null;
    }

    public String buildProductKey(Long id) {
        return PRODUCT_KEY_PREFIX + ":" + id;
    }

    public Product getProduct(Long id) {
        String key = buildProductKey(id);
        String json;
        try {
            json = redisTemplate.opsForValue().get(key);
        } catch (RuntimeException e) {
            log.warn("读取商品缓存失败, id={}", id, e);
            return null;
        }
        if (json == null) {
            return null;
        }

        try {
            Product product = mapper.readValue(json, Product.class);
            if (product.getImages() == null) {
                product.setImages(Collections.emptyList());
            }
            if (product.getSkus() == null) {
                product.setSkus(Collections.emptyList());
            }
            return product;
        } catch (JsonProcessingException e) {
            log.warn("商品缓存反序列化失败", e);
        }
        return null;
    }

    public List<Product> getProducts(List<Long> ids) {
        List<String> keys = ids.stream()
                .map(id -> PRODUCT_KEY_PREFIX + ":" + id)
                .collect(Collectors.toList());
        List<String> jsons;
        try {
            jsons = redisTemplate.opsForValue().multiGet(keys);
        } catch (RuntimeException e) {
            log.warn("批量读取商品缓存失败，降级为数据库查询", e);
            return null;
        }
        if (jsons == null) {
            return null;
        }

        return jsons.stream().map(json -> {
            if (json == null) {
                return null;
            }

            try {
                Product product = mapper.readValue(json, Product.class);
                if (product.getImages() == null) {
                    product.setImages(Collections.emptyList());
                }
                if (product.getSkus() == null) {
                    product.setSkus(Collections.emptyList());
                }
                return product;
            } catch (JsonProcessingException e) {
                log.warn("商品缓存反序列化失败", e);
            }
            return null;
        }).collect(Collectors.toList());
    }

    public void setProduct(Product product) {
        String key = buildProductKey(product.getId());
        try {
            redisTemplate.opsForValue().set(key,
                    mapper.writeValueAsString(product),
                    PRODUCT_CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (JsonProcessingException | RuntimeException e) {
            log.warn("商品缓存写入失败, id={}", product.getId(), e);
        }
    }
}
