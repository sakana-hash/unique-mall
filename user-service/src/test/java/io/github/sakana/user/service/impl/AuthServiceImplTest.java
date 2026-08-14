package io.github.sakana.user.service.impl;

import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.common.properties.JWTProperty;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import io.github.sakana.user.mapper.UserLoginLogMapper;
import io.github.sakana.user.mapper.UserMapper;
import io.github.sakana.user.mapper.UserProfileMapper;
import io.github.sakana.user.pojo.dto.LoginDTO;
import io.github.sakana.user.pojo.dto.RegisterDTO;
import io.github.sakana.user.pojo.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private UserMapper userMapper;
    @Mock
    private UserProfileMapper profileMapper;
    @Mock
    private UserLoginLogMapper loginLogMapper;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Mock
    private JWTProperty jwtProperty;

    @InjectMocks
    private AuthServiceImpl authService;

    @Test
    @DisplayName("注册请求为空时返回稳定业务异常")
    void shouldRejectNullRegisterRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(null)
        );

        assertBusinessError(
                exception, "USER_REGISTER_REQUEST_REQUIRED", "注册信息不能为空", 400
        );
        verifyNoInteractions(
                passwordEncoder,
                userMapper,
                profileMapper,
                loginLogMapper,
                snowflakeIdGenerator,
                jwtProperty
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("用户名为空时返回稳定业务异常")
    void shouldRejectBlankUsername(String username) {
        RegisterDTO request = validRequest();
        request.setUsername(username);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertBusinessError(exception, "USER_USERNAME_REQUIRED", "用户名不能为空", 400);
        verifyNoInteractions(userMapper, profileMapper, loginLogMapper, snowflakeIdGenerator);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("密码为空时返回稳定业务异常")
    void shouldRejectBlankPassword(String password) {
        RegisterDTO request = validRequest();
        request.setPassword(password);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertBusinessError(exception, "USER_PASSWORD_REQUIRED", "密码不能为空", 400);
        verifyNoInteractions(userMapper, profileMapper, loginLogMapper, snowflakeIdGenerator);
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordLengths")
    @DisplayName("注册密码长度不合法时返回稳定业务异常")
    void shouldRejectInvalidRegisterPasswordLength(String password) {
        RegisterDTO request = validRequest();
        request.setPassword(password);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertBusinessError(
                exception,
                "USER_PASSWORD_LENGTH_INVALID",
                "密码长度必须在8到64个字符之间",
                400
        );
        verifyNoInteractions(
                passwordEncoder, userMapper, profileMapper, loginLogMapper, snowflakeIdGenerator
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "password!", "Password-123", "pass word", "密码12345678", "abc.def1", "abc/1234"
    })
    @DisplayName("注册密码包含非法字符时返回稳定业务异常")
    void shouldRejectInvalidRegisterPasswordFormat(String password) {
        RegisterDTO request = validRequest();
        request.setPassword(password);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertBusinessError(
                exception,
                "USER_PASSWORD_FORMAT_INVALID",
                "密码只能由数字、大小写字母和下划线组成",
                400
        );
        verifyNoInteractions(
                passwordEncoder, userMapper, profileMapper, loginLogMapper, snowflakeIdGenerator
        );
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("客户端IP为空时返回稳定业务异常")
    void shouldRejectBlankClientIp(String ip) {
        RegisterDTO request = validRequest();
        request.setIp(ip);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertBusinessError(
                exception, "USER_CLIENT_IP_REQUIRED", "无法获取客户端IP", 400
        );
        verifyNoInteractions(userMapper, profileMapper, loginLogMapper, snowflakeIdGenerator);
    }

    @Test
    @DisplayName("用户名唯一键冲突转换为409业务异常并保留原因")
    void shouldConvertDuplicateUsernameException() {
        RegisterDTO request = validRequest();
        DuplicateKeyException cause = new DuplicateKeyException("duplicate username");
        when(snowflakeIdGenerator.nextId()).thenReturn(1001L);
        when(passwordEncoder.encode("Password_123")).thenReturn("encoded-password");
        when(userMapper.insert(any(User.class))).thenThrow(cause);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.register(request)
        );

        assertBusinessError(
                exception, "USER_USERNAME_ALREADY_EXISTS", "用户名已存在", 409
        );
        assertSame(cause, exception.getCause());
        assertNull(exception.getDetails());
        verify(profileMapper, never()).insert(any());
        verify(loginLogMapper, never()).insert(any());
    }

    @Test
    @DisplayName("登录请求为空时返回稳定业务异常")
    void shouldRejectNullLoginRequest() {
        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(null)
        );

        assertBusinessError(
                exception, "USER_LOGIN_REQUEST_REQUIRED", "登录信息不能为空", 400
        );
        verifyNoInteractions(passwordEncoder, userMapper, loginLogMapper, jwtProperty);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("登录用户名为空时返回稳定业务异常")
    void shouldRejectBlankLoginUsername(String username) {
        LoginDTO request = validLoginRequest();
        request.setUsername(username);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertBusinessError(exception, "USER_USERNAME_REQUIRED", "用户名不能为空", 400);
        verifyNoInteractions(userMapper, loginLogMapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("登录密码为空时返回稳定业务异常")
    void shouldRejectBlankLoginPassword(String password) {
        LoginDTO request = validLoginRequest();
        request.setPassword(password);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertBusinessError(exception, "USER_PASSWORD_REQUIRED", "密码不能为空", 400);
        verifyNoInteractions(userMapper, loginLogMapper);
    }

    @ParameterizedTest
    @MethodSource("invalidPasswordLengths")
    @DisplayName("登录密码长度不合法时返回稳定业务异常")
    void shouldRejectInvalidLoginPasswordLength(String password) {
        LoginDTO request = validLoginRequest();
        request.setPassword(password);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertBusinessError(
                exception,
                "USER_PASSWORD_LENGTH_INVALID",
                "密码长度必须在8到64个字符之间",
                400
        );
        verifyNoInteractions(passwordEncoder, userMapper, loginLogMapper);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "password!", "Password-123", "pass word", "密码12345678", "abc.def1", "abc/1234"
    })
    @DisplayName("登录密码包含非法字符时返回稳定业务异常")
    void shouldRejectInvalidLoginPasswordFormat(String password) {
        LoginDTO request = validLoginRequest();
        request.setPassword(password);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertBusinessError(
                exception,
                "USER_PASSWORD_FORMAT_INVALID",
                "密码只能由数字、大小写字母和下划线组成",
                400
        );
        verifyNoInteractions(passwordEncoder, userMapper, loginLogMapper);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t", "\n"})
    @DisplayName("登录客户端IP为空时返回稳定业务异常")
    void shouldRejectBlankLoginClientIp(String ip) {
        LoginDTO request = validLoginRequest();
        request.setIp(ip);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertBusinessError(
                exception, "USER_CLIENT_IP_REQUIRED", "无法获取客户端IP", 400
        );
        verifyNoInteractions(userMapper, loginLogMapper);
    }

    @Test
    @DisplayName("用户名不存在时统一返回401凭证错误")
    void shouldRejectUnknownUsername() {
        LoginDTO request = validLoginRequest();
        when(userMapper.selectByUsername("sakana")).thenReturn(null);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertBusinessError(
                exception, "USER_INVALID_CREDENTIALS", "用户名或密码错误", 401
        );
        verify(passwordEncoder, never()).matches(any(), any());
        verify(loginLogMapper, never()).insert(any());
    }

    @Test
    @DisplayName("密码错误时统一返回401凭证错误")
    void shouldRejectWrongPassword() {
        LoginDTO request = validLoginRequest();
        User user = new User();
        user.setId(1001L);
        user.setPassword("encoded-password");
        when(userMapper.selectByUsername("sakana")).thenReturn(user);
        when(passwordEncoder.matches("Password_123", "encoded-password")).thenReturn(false);

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> authService.login(request)
        );

        assertBusinessError(
                exception, "USER_INVALID_CREDENTIALS", "用户名或密码错误", 401
        );
        verify(loginLogMapper, never()).insert(any());
    }

    private static RegisterDTO validRequest() {
        RegisterDTO request = new RegisterDTO();
        request.setUsername("sakana");
        request.setPassword("Password_123");
        request.setIp("127.0.0.1");
        request.setDevice("desktop");
        return request;
    }

    private static LoginDTO validLoginRequest() {
        LoginDTO request = new LoginDTO();
        request.setUsername("sakana");
        request.setPassword("Password_123");
        request.setIp("127.0.0.1");
        request.setDevice("desktop");
        return request;
    }

    private static Stream<String> invalidPasswordLengths() {
        return Stream.of("Abc_123", "A".repeat(65));
    }

    private static void assertBusinessError(
            BusinessException exception,
            String expectedCode,
            String expectedMessage,
            int expectedHttpStatus
    ) {
        assertEquals(expectedCode, exception.getCode());
        assertEquals(expectedMessage, exception.getMessage());
        assertEquals(expectedHttpStatus, exception.getHttpStatus());
    }
}
