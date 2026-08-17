package io.github.sakana.product.enumeration;

import io.github.sakana.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductErrorCodeTest {

    @ParameterizedTest
    @EnumSource(ProductErrorCode.class)
    @DisplayName("所有商品错误码定义均合法")
    void shouldHaveValidDefinition(ProductErrorCode errorCode) {
        assertFalse(errorCode.getCode().isBlank());
        assertFalse(errorCode.getMessage().isBlank());
        assertTrue(errorCode.getHttpStatus() >= 400 && errorCode.getHttpStatus() < 500);
    }

    @Test
    @DisplayName("商品服务错误码不能重复")
    void shouldHaveUniqueCodes() {
        Set<String> codes = new HashSet<>();

        for (ProductErrorCode errorCode : ProductErrorCode.values()) {
            assertTrue(codes.add(errorCode.getCode()), "重复错误码: " + errorCode.getCode());
        }
    }

    @Test
    @DisplayName("商品详情错误码契约保持稳定")
    void shouldExposeExpectedContracts() {
        assertError(ProductErrorCode.PRODUCT_ID_INVALID,
                "PRODUCT_ID_INVALID", "商品ID不合法", 400);
        assertError(ProductErrorCode.PRODUCT_NOT_FOUND,
                "PRODUCT_NOT_FOUND", "商品不存在", 404);
        assertError(ProductErrorCode.PRODUCT_NOT_ON_SALE,
                "PRODUCT_NOT_ON_SALE", "商品已下架", 409);
    }

    @Test
    @DisplayName("能够创建携带商品上下文的业务异常")
    void shouldCreateBusinessExceptionWithDetails() {
        Map<String, Long> details = Map.of("productId", 1001L);

        BusinessException exception = ProductErrorCode.PRODUCT_NOT_FOUND.exception(details);

        assertEquals("PRODUCT_NOT_FOUND", exception.getCode());
        assertEquals("商品不存在", exception.getMessage());
        assertEquals(404, exception.getHttpStatus());
        assertSame(details, exception.getDetails());
    }

    private static void assertError(
            ProductErrorCode errorCode,
            String expectedCode,
            String expectedMessage,
            int expectedHttpStatus
    ) {
        assertEquals(expectedCode, errorCode.getCode());
        assertEquals(expectedMessage, errorCode.getMessage());
        assertEquals(expectedHttpStatus, errorCode.getHttpStatus());
    }
}
