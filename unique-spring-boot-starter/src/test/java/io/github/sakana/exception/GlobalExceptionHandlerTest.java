package io.github.sakana.exception;

import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.config.ExceptionHandlerAutoConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("业务异常返回指定HTTP状态、业务错误码和详情")
    void shouldHandleBusinessException() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.msg").value("商品不存在"))
                .andExpect(jsonPath("$.details.productId").value(1001));
    }

    @Test
    @DisplayName("未预期异常返回500且不泄露内部异常信息")
    void shouldHandleUnexpectedException() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value(1))
                .andExpect(jsonPath("$.errorCode").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.msg").value("系统繁忙，请稍后重试"));
    }

    @Test
    @DisplayName("缺少请求头时返回统一的400响应")
    void shouldHandleMissingRequestHeader() throws Exception {
        mockMvc.perform(get("/test/header"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_INVALID"))
                .andExpect(jsonPath("$.msg").value("请求参数格式错误"));
    }

    @Test
    @DisplayName("无法解析JSON时返回统一的400响应")
    void shouldHandleUnreadableRequestBody() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("REQUEST_INVALID"));
    }

    @Test
    @DisplayName("不支持的请求方法返回405")
    void shouldHandleMethodNotAllowed() throws Exception {
        mockMvc.perform(post("/test/business"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.errorCode").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    @DisplayName("不支持的内容类型返回415")
    void shouldHandleUnsupportedMediaType() throws Exception {
        mockMvc.perform(post("/test/body")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("plain text"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.errorCode").value("MEDIA_TYPE_NOT_SUPPORTED"));
    }

    @Test
    @DisplayName("不存在的资源保持404语义")
    void shouldHandleResourceNotFound() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        var response = handler.handleResourceNotFound(
                new NoResourceFoundException(HttpMethod.GET, "/missing")
        );

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().errorCode()).isEqualTo("RESOURCE_NOT_FOUND");
    }

    @Test
    @DisplayName("Servlet应用自动注册全局异常处理器")
    void shouldAutoConfigureHandler() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        ExceptionHandlerAutoConfiguration.class
                ))
                .run(context -> assertThat(context)
                        .hasSingleBean(GlobalExceptionHandler.class));
    }

    @RestController
    @RequestMapping("/test")
    static class TestController {

        @GetMapping("/business")
        void business() {
            throw new BusinessException(
                    "PRODUCT_NOT_FOUND",
                    "商品不存在",
                    404,
                    Map.of("productId", 1001L),
                    null
            );
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("database password: secret");
        }

        @GetMapping("/header")
        String header(@RequestHeader("X-Test") String header) {
            return header;
        }

        @PostMapping(value = "/body", consumes = MediaType.APPLICATION_JSON_VALUE)
        void body(@RequestBody Map<String, Object> body) {
        }
    }
}
