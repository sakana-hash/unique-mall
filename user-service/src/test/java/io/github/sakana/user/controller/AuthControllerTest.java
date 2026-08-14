package io.github.sakana.user.controller;

import io.github.sakana.common.constant.HeadersConstant;
import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.exception.GlobalExceptionHandler;
import io.github.sakana.user.enumeration.UserErrorCode;
import io.github.sakana.user.pojo.dto.LoginDTO;
import io.github.sakana.user.pojo.dto.RegisterDTO;
import io.github.sakana.user.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerTest {

    private final StubAuthService authService = new StubAuthService();
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        AuthController controller = new AuthController();
        ReflectionTestUtils.setField(controller, "authService", authService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("注册请求体为null时返回统一的400参数错误")
    void shouldRejectNullRegisterBody() throws Exception {
        mockMvc.perform(post("/api/user/auth/register")
                        .header(HeadersConstant.User_IP, "127.0.0.1")
                        .header(HeadersConstant.USER_DEVICE, "desktop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALID"))
                .andExpect(jsonPath("$.msg").value("请求参数格式错误"));
    }

    @Test
    @DisplayName("用户名重复时返回统一的409业务错误")
    void shouldReturnDuplicateUsernameError() throws Exception {
        authService.registerFailure = UserErrorCode.USERNAME_ALREADY_EXISTS.exception();

        mockMvc.perform(post("/api/user/auth/register")
                        .header(HeadersConstant.User_IP, "127.0.0.1")
                        .header(HeadersConstant.USER_DEVICE, "desktop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sakana\",\"password\":\"password\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("USER_USERNAME_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.msg").value("用户名已存在"));
    }

    @Test
    @DisplayName("注册成功时code为SUCCESS")
    void shouldReturnSuccessCode() throws Exception {
        mockMvc.perform(post("/api/user/auth/register")
                        .header(HeadersConstant.User_IP, "127.0.0.1")
                        .header(HeadersConstant.USER_DEVICE, "desktop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sakana\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.msg").value("成功"))
                .andExpect(jsonPath("$.data.token").value("token"));
    }

    @Test
    @DisplayName("登录请求体为null时返回统一的400参数错误")
    void shouldRejectNullLoginBody() throws Exception {
        mockMvc.perform(post("/api/user/auth/login")
                        .header(HeadersConstant.User_IP, "127.0.0.1")
                        .header(HeadersConstant.USER_DEVICE, "desktop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("null"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_INVALID"))
                .andExpect(jsonPath("$.msg").value("请求参数格式错误"));
    }

    @Test
    @DisplayName("登录凭证错误时返回统一的401业务错误")
    void shouldReturnInvalidCredentialsError() throws Exception {
        authService.loginFailure = UserErrorCode.INVALID_CREDENTIALS.exception();

        mockMvc.perform(post("/api/user/auth/login")
                        .header(HeadersConstant.User_IP, "127.0.0.1")
                        .header(HeadersConstant.USER_DEVICE, "desktop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sakana\",\"password\":\"wrong\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("USER_INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.msg").value("用户名或密码错误"));
    }

    @Test
    @DisplayName("登录成功时code为SUCCESS")
    void shouldReturnLoginSuccessCode() throws Exception {
        mockMvc.perform(post("/api/user/auth/login")
                        .header(HeadersConstant.User_IP, "127.0.0.1")
                        .header(HeadersConstant.USER_DEVICE, "desktop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"sakana\",\"password\":\"password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("SUCCESS"))
                .andExpect(jsonPath("$.data.token").value("token"));
    }

    private static class StubAuthService implements AuthService {

        private BusinessException registerFailure;
        private BusinessException loginFailure;

        @Override
        public String register(RegisterDTO registerDTO) {
            if (registerFailure != null) {
                throw registerFailure;
            }
            return "token";
        }

        @Override
        public String login(LoginDTO loginDTO) {
            if (loginFailure != null) {
                throw loginFailure;
            }
            return "token";
        }
    }
}
