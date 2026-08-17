package io.github.sakana.product.service;

import io.github.sakana.product.pojo.entity.PageResult;
import io.github.sakana.product.pojo.entity.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.sakana.product.constant.ProductConstants.PAGE_RESULT_CACHE_TTL_SECONDS;
import static io.github.sakana.product.constant.ProductConstants.PRODUCT_CACHE_TTL_SECONDS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CacheService cacheService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("分页结果缓存使用统一的TTL")
    void shouldUsePageResultCacheTtl() {
        PageResult pageResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .build();

        cacheService.setPageResult("page-key", pageResult);

        verify(valueOperations).set(
                eq("page-key"),
                anyString(),
                eq(PAGE_RESULT_CACHE_TTL_SECONDS),
                eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("商品缓存使用统一的TTL")
    void shouldUseProductCacheTtl() {
        Product product = new Product();
        product.setId(1001L);
        String productKey = cacheService.buildProductKey(product.getId());

        cacheService.setProduct(product);

        verify(valueOperations).set(
                eq(productKey),
                anyString(),
                eq(PRODUCT_CACHE_TTL_SECONDS),
                eq(TimeUnit.SECONDS)
        );
    }
}
