package io.github.sakana.product.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sakana.product.enumeration.PageSort;
import io.github.sakana.product.pojo.entity.PageResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class CacheService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    private static final ObjectMapper mapper = new ObjectMapper();

    private static final String PRODUCT_KEY_PREFIX = "product-service:product";
    private static final long PRODUCT_CACHE_EXPIRE_TIME = 600;

    private static final String PAGE_RESULT_KEY_PREFIX = "product-service:page";
    private static final long PAGE_RESULT_CACHE_EXPIRE_TIME = 60;


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
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) {
            return null;
        }

        try {
            return mapper.readValue(json, PageResult.class);
        } catch (JsonProcessingException e) {
            log.warn("分类结果缓存序列化失败");
        }
        return null;
    }

    public void setPageResult(String key, PageResult result) {
        try {
            redisTemplate.opsForValue().set(key,
                    mapper.writeValueAsString(result),
                    PAGE_RESULT_CACHE_EXPIRE_TIME, TimeUnit.SECONDS);
        } catch (JsonProcessingException e) {
            log.warn("分页结果写入缓存失败", e);
        }
    }
}
