package io.github.sakana.user.enumeration;

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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserErrorCodeTest {

    @ParameterizedTest
    @EnumSource(UserErrorCode.class)
    @DisplayName("所有用户错误码定义均合法")
    void shouldHaveValidDefinition(UserErrorCode errorCode) {
        assertTrue(errorCode.getCode().startsWith("USER_"));
        assertTrue(errorCode.getHttpStatus() >= 400 && errorCode.getHttpStatus() < 500);
        assertFalse(errorCode.getMessage().isBlank());
    }

    @Test
    @DisplayName("用户服务内的错误码不能重复")
    void shouldHaveUniqueCodes() {
        Set<String> codes = new HashSet<>();

        for (UserErrorCode errorCode : UserErrorCode.values()) {
            assertTrue(codes.add(errorCode.getCode()), "重复错误码: " + errorCode.getCode());
        }
    }

    @Test
    @DisplayName("错误码、消息和HTTP状态保持稳定")
    void shouldExposeExpectedContracts() {
        assertError(UserErrorCode.REGISTER_REQUEST_REQUIRED,
                "USER_REGISTER_REQUEST_REQUIRED", "注册信息不能为空", 400);
        assertError(UserErrorCode.LOGIN_REQUEST_REQUIRED,
                "USER_LOGIN_REQUEST_REQUIRED", "登录信息不能为空", 400);
        assertError(UserErrorCode.USERNAME_REQUIRED,
                "USER_USERNAME_REQUIRED", "用户名不能为空", 400);
        assertError(UserErrorCode.PASSWORD_REQUIRED,
                "USER_PASSWORD_REQUIRED", "密码不能为空", 400);
        assertError(UserErrorCode.PASSWORD_LENGTH_INVALID,
                "USER_PASSWORD_LENGTH_INVALID", "密码长度必须在8到64个字符之间", 400);
        assertError(UserErrorCode.PASSWORD_FORMAT_INVALID,
                "USER_PASSWORD_FORMAT_INVALID", "密码只能由数字、大小写字母和下划线组成", 400);
        assertError(UserErrorCode.CLIENT_IP_REQUIRED,
                "USER_CLIENT_IP_REQUIRED", "无法获取客户端IP", 400);
        assertError(UserErrorCode.USERNAME_ALREADY_EXISTS,
                "USER_USERNAME_ALREADY_EXISTS", "用户名已存在", 409);
        assertError(UserErrorCode.INVALID_CREDENTIALS,
                "USER_INVALID_CREDENTIALS", "用户名或密码错误", 401);
        assertError(UserErrorCode.ADDRESS_ID_INVALID,
                "USER_ADDRESS_ID_INVALID", "收货地址ID不合法", 400);
        assertError(UserErrorCode.USER_ID_INVALID,
                "USER_ID_INVALID", "用户ID不合法", 400);
        assertError(UserErrorCode.ADDRESS_NOT_FOUND,
                "USER_ADDRESS_NOT_FOUND", "收货地址不存在", 404);
    }

    @Test
    @DisplayName("能够创建不带上下文的业务异常")
    void shouldCreateBusinessException() {
        BusinessException exception = UserErrorCode.INVALID_CREDENTIALS.exception();

        assertEquals("USER_INVALID_CREDENTIALS", exception.getCode());
        assertEquals("用户名或密码错误", exception.getMessage());
        assertEquals(401, exception.getHttpStatus());
        assertNull(exception.getDetails());
        assertNull(exception.getCause());
    }

    @Test
    @DisplayName("能够将详情和原始异常传递给业务异常")
    void shouldCreateBusinessExceptionWithContext() {
        Map<String, Object> details = Map.of("username", "sakana");
        RuntimeException cause = new RuntimeException("duplicate key");

        BusinessException exception = UserErrorCode.USERNAME_ALREADY_EXISTS
                .exception(details, cause);

        assertEquals("USER_USERNAME_ALREADY_EXISTS", exception.getCode());
        assertEquals(409, exception.getHttpStatus());
        assertSame(details, exception.getDetails());
        assertSame(cause, exception.getCause());
    }

    private static void assertError(
            UserErrorCode errorCode,
            String expectedCode,
            String expectedMessage,
            int expectedHttpStatus
    ) {
        assertEquals(expectedCode, errorCode.getCode());
        assertEquals(expectedMessage, errorCode.getMessage());
        assertEquals(expectedHttpStatus, errorCode.getHttpStatus());
    }
}
