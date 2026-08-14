package io.github.sakana.common.result;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ResultTest {

    @Test
    @DisplayName("无数据成功响应使用SUCCESS错误码")
    void shouldCreateSuccessWithoutData() {
        Result<Object> result = Result.success();

        assertEquals("SUCCESS", result.getCode());
        assertEquals("成功", result.getMsg());
        assertNull(result.getData());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("成功响应保留数据")
    void shouldCreateSuccessWithData() {
        Map<String, Object> data = Map.of("id", 1001L);

        Result<Map<String, Object>> result = Result.success(data);

        assertEquals("SUCCESS", result.getCode());
        assertSame(data, result.getData());
        assertTrue(result.isSuccess());
    }

    @Test
    @DisplayName("错误响应直接将业务错误码写入code")
    void shouldCreateErrorWithoutDetails() {
        Result<Object> result = Result.error("PRODUCT_NOT_FOUND", "商品不存在");

        assertEquals("PRODUCT_NOT_FOUND", result.getCode());
        assertEquals("商品不存在", result.getMsg());
        assertNull(result.getData());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("错误详情通过data返回")
    void shouldCreateErrorWithDetails() {
        Map<String, Object> details = Map.of("productId", 1001L);

        Result<Map<String, Object>> result = Result.error(
                "PRODUCT_NOT_FOUND", "商品不存在", details
        );

        assertSame(details, result.getData());
        assertFalse(result.isSuccess());
    }

    @Test
    @DisplayName("code为空的未初始化响应不是成功响应")
    void shouldTreatNullCodeAsFailure() {
        assertFalse(new Result<>().isSuccess());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("错误码不能为空")
    void shouldRejectBlankErrorCode(String errorCode) {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> Result.error(errorCode, "业务失败")
        );

        assertEquals("errorCode不能为空", exception.getMessage());
    }
}
