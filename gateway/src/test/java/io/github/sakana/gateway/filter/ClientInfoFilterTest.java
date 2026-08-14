package io.github.sakana.gateway.filter;

import io.github.sakana.common.constant.HeadersConstant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ClientInfoFilterTest {

    private final ClientInfoFilter filter = new ClientInfoFilter();

    @Test
    @DisplayName("优先使用X-Real-IP并覆盖客户端伪造信息")
    void shouldPreferRealIpAndReplaceSpoofedHeaders() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/orders")
                .header("X-Real-IP", "203.0.113.10")
                .header("X-Forwarded-For", "198.51.100.20, 10.0.0.1")
                .header(HttpHeaders.USER_AGENT, iphoneUserAgent())
                .header(HeadersConstant.USER_IP, "attacker", "another-value")
                .header(HeadersConstant.USER_DEVICE, "spoofed-device", "another-value")
                .build());

        ServerWebExchange chainedExchange = apply(exchange);
        HttpHeaders headers = chainedExchange.getRequest().getHeaders();

        assertEquals("203.0.113.10", headers.getFirst(HeadersConstant.USER_IP));
        assertEquals(List.of("203.0.113.10"), headers.get(HeadersConstant.USER_IP));
        assertEquals("iPhone", headers.getFirst(HeadersConstant.USER_DEVICE));
        assertEquals(List.of("iPhone"), headers.get(HeadersConstant.USER_DEVICE));
    }

    @Test
    @DisplayName("X-Real-IP无效时使用转发链中第一个有效地址")
    void shouldUseFirstValidForwardedIp() {
        MockServerWebExchange exchange = exchange(MockServerHttpRequest.get("/api/orders")
                .header("X-Real-IP", "unknown")
                .header("X-Forwarded-For", " unknown, , 198.51.100.20, 10.0.0.1 ")
                .build());

        ServerWebExchange chainedExchange = apply(exchange);

        assertEquals(
                "198.51.100.20",
                chainedExchange.getRequest().getHeaders().getFirst(HeadersConstant.USER_IP)
        );
    }

    @Test
    @DisplayName("缺少IP和User-Agent时写入unknown")
    void shouldUseUnknownWhenClientInfoIsMissing() {
        MockServerWebExchange exchange = exchange(
                MockServerHttpRequest.get("/api/orders").build()
        );

        ServerWebExchange chainedExchange = apply(exchange);
        HttpHeaders headers = chainedExchange.getRequest().getHeaders();

        assertEquals("unknown", headers.getFirst(HeadersConstant.USER_IP));
        assertEquals("unknown", headers.getFirst(HeadersConstant.USER_DEVICE));
    }

    @Test
    @DisplayName("客户端信息过滤器在鉴权过滤器之后执行")
    void shouldHaveExpectedOrder() {
        assertEquals(1, filter.getOrder());
    }

    private ServerWebExchange apply(MockServerWebExchange exchange) {
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();

        filter.filter(exchange, chainedExchange -> {
            captured.set(chainedExchange);
            return reactor.core.publisher.Mono.empty();
        }).block();

        return captured.get();
    }

    private static MockServerWebExchange exchange(MockServerHttpRequest request) {
        return MockServerWebExchange.from(request);
    }

    private static String iphoneUserAgent() {
        return "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) "
                + "AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.0 "
                + "Mobile/15E148 Safari/604.1";
    }
}
