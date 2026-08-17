package io.github.sakana.product.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static io.github.sakana.product.constant.ProductConstants.PAGE_RESULT_CACHE_PHYSICAL_TTL_SECONDS;
import static io.github.sakana.product.constant.ProductConstants.PAGE_RESULT_CACHE_TTL_SECONDS;
import static io.github.sakana.product.constant.ProductConstants.PRODUCT_CACHE_TTL_SECONDS;
import static io.github.sakana.product.constant.ProductConstants.MIN_VALID_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CacheServiceTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
    @DisplayName("分页结果缓存使用逻辑TTL和较长的物理TTL")
    void shouldUseLogicalAndPhysicalPageResultCacheTtl() {
        PageResult pageResult = PageResult.builder()
                .ids(List.of())
                .total(0L)
                .build();

        LocalDateTime beforeWrite = LocalDateTime.now();
        cacheService.setPageResult("page-key", pageResult);

        verify(valueOperations).set(
                eq("page-key"),
                anyString(),
                eq(PAGE_RESULT_CACHE_PHYSICAL_TTL_SECONDS),
                eq(TimeUnit.SECONDS)
        );
        assertNotNull(pageResult.getExpireTime());
        assertTrue(pageResult.getExpireTime().isAfter(
                beforeWrite.plusSeconds(PAGE_RESULT_CACHE_TTL_SECONDS - 1)
        ));
        assertTrue(pageResult.getExpireTime().isBefore(
                LocalDateTime.now().plusSeconds(PAGE_RESULT_CACHE_TTL_SECONDS + 1)
        ));
    }

    @Test
    @DisplayName("完整但逻辑过期的分页缓存仍然可以返回")
    void shouldReturnCompleteExpiredPageResult() throws Exception {
        PageResult expired = PageResult.builder()
                .ids(List.of(1001L))
                .total(1L)
                .expireTime(LocalDateTime.now().minusSeconds(1))
                .build();
        when(valueOperations.get("page-key"))
                .thenReturn(OBJECT_MAPPER.writeValueAsString(expired));

        PageResult result = cacheService.getPageResult("page-key");

        assertNotNull(result);
        assertEquals(expired.getIds(), result.getIds());
        assertEquals(expired.getTotal(), result.getTotal());
        assertEquals(expired.getExpireTime(), result.getExpireTime());
    }

    @Test
    @DisplayName("结构不完整的分页缓存按未命中处理")
    void shouldRejectIncompletePageResult() {
        when(valueOperations.get("page-key"))
                .thenReturn("{\"ids\":[1001],\"total\":1}");

        assertNull(cacheService.getPageResult("page-key"));
    }

    @Test
    @DisplayName("包含非法商品ID的分页缓存按未命中处理")
    void shouldRejectPageResultWithInvalidProductId() throws Exception {
        PageResult invalid = PageResult.builder()
                .ids(List.of(MIN_VALID_ID - 1))
                .total(1L)
                .expireTime(LocalDateTime.now().plusMinutes(1))
                .build();
        when(valueOperations.get("page-key"))
                .thenReturn(OBJECT_MAPPER.writeValueAsString(invalid));

        assertNull(cacheService.getPageResult("page-key"));
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
