package io.github.sakana.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sakana.common.constant.HeadersConstant;
import io.github.sakana.common.properties.JWTProperty;
import io.github.sakana.common.utils.JWTUtil;
import io.github.sakana.gateway.properties.AuthProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthGlobalFilterTest {

    private static final String SECRET_KEY = "0123456789abcdef0123456789abcdef";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private AuthGlobalFilter filter;

    @BeforeEach
    void setUp() {
        JWTProperty jwtProperty = new JWTProperty();
        jwtProperty.setSecretKey(SECRET_KEY);

        AuthProperties authProperties = new AuthProperties();
        authProperties.setIgnoreUrls(List.of("/api/user/auth/**", "/public/*"));

        filter = new AuthGlobalFilter();
        ReflectionTestUtils.setField(filter, "jwtProperty", jwtProperty);
        ReflectionTestUtils.setField(filter, "authProperties", authProperties);
        ReflectionTestUtils.setField(filter, "objectMapper", objectMapper);
    }

    @Test
    @DisplayName("白名单路径不校验Token并直接放行")
    void shouldPassIgnoredPathWithoutToken() {
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.get("/api/user/auth/login").build()
        );
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, chainedExchange -> {
            captured.set(chainedExchange);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertSame(exchange, captured.get());
        assertNull(exchange.getResponse().getStatusCode());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"Basic abc", "Token abc", "Bearer    "})
    @DisplayName("缺失或格式错误的Authorization头返回401")
    void shouldRejectMissingAuthorization(String authorization) throws Exception {
        MockServerHttpRequest.BaseBuilder<?> request = MockServerHttpRequest.get("/api/orders");
        if (authorization != null) {
            request.header(HttpHeaders.AUTHORIZATION, authorization);
        }
        MockServerWebExchange exchange = exchange(request.build());
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, chainedExchange -> {
            captured.set(chainedExchange);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertUnauthorized(exchange, "缺少认证凭证");
        assertNull(captured.get());
    }

    @Test
    @DisplayName("无效Token返回401且不进入下游")
    void shouldRejectInvalidToken() throws Exception {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build());
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, chainedExchange -> {
            captured.set(chainedExchange);
            return reactor.core.publisher.Mono.empty();
        }).block();

        assertUnauthorized(exchange, "认证凭证无效或已过期，请重新登录");
        assertNull(captured.get());
    }

    @Test
    @DisplayName("过期Token返回401")
    void shouldRejectExpiredToken() throws Exception {
        String token = JWTUtil.issueJWT(SECRET_KEY, -60_000, "1001");
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());

        filter.filter(exchange, chainedExchange -> reactor.core.publisher.Mono.empty()).block();

        assertUnauthorized(exchange, "认证凭证无效或已过期，请重新登录");
    }

    @Test
    @DisplayName("有效Token覆盖客户端伪造的用户ID并放行")
    void shouldReplaceSpoofedUserIdWithTokenSubject() {
        String token = JWTUtil.issueJWT(SECRET_KEY, 60_000, "1001");
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "bEaReR " + token)
                .header(HeadersConstant.USER_ID, "attacker", "another-value")
                .build());
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, chainedExchange -> {
            captured.set(chainedExchange);
            return reactor.core.publisher.Mono.empty();
        }).block();

        HttpHeaders headers = captured.get().getRequest().getHeaders();
        assertEquals("1001", headers.getFirst(HeadersConstant.USER_ID));
        assertEquals(List.of("1001"), headers.get(HeadersConstant.USER_ID));
        assertNull(exchange.getResponse().getStatusCode());
    }

    @Test
    @DisplayName("JSON序列化失败时仍返回合法的兜底响应")
    void shouldUseFallbackBodyWhenSerializationFails() throws Exception {
        ObjectMapper failingObjectMapper = new ObjectMapper() {
            @Override
            public byte[] writeValueAsBytes(Object value) throws JsonProcessingException {
                throw new JsonProcessingException("serialization failed") {
                };
            }
        };
        ReflectionTestUtils.setField(filter, "objectMapper", failingObjectMapper);
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.get("/api/orders").build()
        );

        filter.filter(exchange, chainedExchange -> reactor.core.publisher.Mono.empty()).block();

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertEquals("AUTH_UNAUTHORIZED", body.get("code").asText());
        assertEquals("认证失败", body.get("msg").asText());
        assertTrue(body.get("data").isNull());
    }

    @Test
    @DisplayName("鉴权过滤器优先执行")
    void shouldHaveExpectedOrder() {
        assertEquals(-100, filter.getOrder());
    }

    private void assertUnauthorized(MockServerWebExchange exchange, String message)
            throws Exception {
        assertEquals(HttpStatus.UNAUTHORIZED, exchange.getResponse().getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON, exchange.getResponse().getHeaders().getContentType());

        JsonNode body = objectMapper.readTree(exchange.getResponse().getBodyAsString().block());
        assertEquals("AUTH_UNAUTHORIZED", body.get("code").asText());
        assertEquals(message, body.get("msg").asText());
        assertTrue(body.get("data").isNull());
        assertFalse(body.get("success").asBoolean());
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest request) {
        return MockServerWebExchange.from(request);
    }
}
