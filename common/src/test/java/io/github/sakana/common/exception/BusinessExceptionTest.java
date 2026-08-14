package io.github.sakana.common.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class BusinessExceptionTest {

    @Nested
    @DisplayName("构造异常")
    class ConstructorTests {

        @Test
        @DisplayName("仅提供消息时使用默认业务错误码")
        void shouldUseDefaultCode() {
            BusinessException exception = new BusinessException("商品不存在");

            assertException(exception, BusinessException.DEFAULT_CODE, "商品不存在", null, null);
        }

        @Test
        @DisplayName("默认业务错误码构造器能够保留原始异常")
        void shouldUseDefaultCodeAndRetainCause() {
            IllegalStateException cause = new IllegalStateException("商品数据异常");

            BusinessException exception = new BusinessException("商品查询失败", cause);

            assertException(exception, BusinessException.DEFAULT_CODE, "商品查询失败", null, cause);
        }

        @Test
        @DisplayName("能够指定业务错误码和消息")
        void shouldUseExplicitCode() {
            BusinessException exception = new BusinessException(
                    "PRODUCT_NOT_FOUND", "商品不存在"
            );

            assertException(exception, "PRODUCT_NOT_FOUND", "商品不存在", null, null);
        }

        @Test
        @DisplayName("能够指定业务异常对应的HTTP状态码")
        void shouldUseExplicitHttpStatus() {
            BusinessException exception = new BusinessException(
                    "PRODUCT_NOT_FOUND", "商品不存在", 404
            );

            assertEquals(404, exception.getHttpStatus());
            assertEquals("PRODUCT_NOT_FOUND", exception.getCode());
            assertEquals("商品不存在", exception.getMessage());
        }

        @Test
        @DisplayName("能够携带结构化详情")
        void shouldRetainDetails() {
            Map<String, Object> details = Map.of("skuId", 1001L, "available", 0);

            BusinessException exception = new BusinessException(
                    "STOCK_INSUFFICIENT", "库存不足", details
            );

            assertException(exception, "STOCK_INSUFFICIENT", "库存不足", details, null);
        }

        @Test
        @DisplayName("指定业务错误码的构造器能够保留原始异常")
        void shouldRetainCauseWithExplicitCode() {
            RuntimeException cause = new RuntimeException("database error");

            BusinessException exception = new BusinessException(
                    "ORDER_PERSISTENCE_FAILED", "订单保存失败", cause
            );

            assertException(
                    exception, "ORDER_PERSISTENCE_FAILED", "订单保存失败", null, cause
            );
        }

        @Test
        @DisplayName("完整构造器保留业务错误码、详情和原始异常")
        void shouldRetainAllContext() {
            Map<String, Object> details = Map.of("skuId", 1001L, "available", 0);
            RuntimeException cause = new RuntimeException("database error");

            BusinessException exception = new BusinessException(
                    "STOCK_INSUFFICIENT", "库存不足", details, cause
            );

            assertException(exception, "STOCK_INSUFFICIENT", "库存不足", details, cause);
        }

        @Test
        @DisplayName("完整构造器能够指定HTTP状态码")
        void shouldRetainAllContextWithExplicitHttpStatus() {
            Map<String, Object> details = Map.of("productId", 1001L);
            RuntimeException cause = new RuntimeException("database error");

            BusinessException exception = new BusinessException(
                    "PRODUCT_NOT_FOUND", "商品不存在", 404, details, cause
            );

            assertEquals(404, exception.getHttpStatus());
            assertEquals("PRODUCT_NOT_FOUND", exception.getCode());
            assertEquals("商品不存在", exception.getMessage());
            assertSame(details, exception.getDetails());
            assertSame(cause, exception.getCause());
        }
    }

    @Nested
    @DisplayName("参数校验")
    class ValidationTests {

        @ParameterizedTest(name = "错误码为 [{0}] 时拒绝创建")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void shouldRejectBlankCode(String code) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new BusinessException(code, "库存不足")
            );

            assertEquals("code不能为空", exception.getMessage());
        }

        @ParameterizedTest(name = "消息为 [{0}] 时拒绝创建")
        @NullAndEmptySource
        @ValueSource(strings = {" ", "\t", "\n"})
        void shouldRejectBlankMessage(String message) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new BusinessException("STOCK_INSUFFICIENT", message)
            );

            assertEquals("message不能为空", exception.getMessage());
        }

        @ParameterizedTest(name = "HTTP状态码为 [{0}] 时拒绝创建")
        @ValueSource(ints = {0, 200, 399, 600, 999})
        void shouldRejectNonErrorHttpStatus(int httpStatus) {
            IllegalArgumentException exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> new BusinessException("BUSINESS_ERROR", "业务失败", httpStatus)
            );

            assertEquals(
                    "httpStatus必须是400到599之间的错误状态码",
                    exception.getMessage()
            );
        }
    }

    @Nested
    @DisplayName("序列化")
    class SerializationTests {

        @Test
        @DisplayName("声明稳定的序列化版本号")
        void shouldDeclareStableSerialVersionUid() {
            long serialVersionUid = ObjectStreamClass.lookup(BusinessException.class)
                    .getSerialVersionUID();

            assertEquals(1L, serialVersionUid);
        }

        @Test
        @DisplayName("序列化保留错误信息和原因，但忽略 transient 详情")
        void shouldSerializeWithoutDetails() throws Exception {
            Object nonSerializableDetails = new Object();
            BusinessException original = new BusinessException(
                    "STOCK_INSUFFICIENT",
                    "库存不足",
                    nonSerializableDetails,
                    new IllegalStateException("库存记录异常")
            );

            BusinessException restored = roundTrip(original);

            assertEquals("STOCK_INSUFFICIENT", restored.getCode());
            assertEquals("库存不足", restored.getMessage());
            assertEquals(BusinessException.DEFAULT_HTTP_STATUS, restored.getHttpStatus());
            assertNull(restored.getDetails());
            IllegalStateException restoredCause = assertInstanceOf(
                    IllegalStateException.class, restored.getCause()
            );
            assertEquals("库存记录异常", restoredCause.getMessage());
        }
    }

    private static void assertException(
            BusinessException exception,
            String expectedCode,
            String expectedMessage,
            Object expectedDetails,
            Throwable expectedCause
    ) {
        assertEquals(expectedCode, exception.getCode());
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(BusinessException.DEFAULT_HTTP_STATUS, exception.getHttpStatus());
        assertSame(expectedDetails, exception.getDetails());
        assertSame(expectedCause, exception.getCause());
    }

    private static BusinessException roundTrip(BusinessException exception) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream output = new ObjectOutputStream(bytes)) {
            output.writeObject(exception);
        }

        try (ObjectInputStream input = new ObjectInputStream(
                new ByteArrayInputStream(bytes.toByteArray())
        )) {
            return (BusinessException) input.readObject();
        }
    }
}
