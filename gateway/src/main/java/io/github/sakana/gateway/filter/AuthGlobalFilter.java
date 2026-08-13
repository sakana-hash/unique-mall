package io.github.sakana.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.sakana.common.constant.HeadersConstant;
import io.github.sakana.common.properties.JWTProperty;
import io.github.sakana.common.result.Result;
import io.github.sakana.common.utils.JWTUtil;
import io.github.sakana.gateway.properties.AuthProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

/**
 * 全局鉴权过滤器：
 * 1. 白名单路径直接放行；
 * 2. 其余请求从 Authorization: Bearer xxx 中提取 JWT 并验签、验过期；
 * 3. 校验通过后把用户身份（userId）以 X-User-Id 请求头透传给下游服务。
 */
@Component
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final String HEADER_USER_ID = HeadersConstant.USER_ID;

    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    @Autowired
    private JWTProperty jwtProperty;
    @Autowired
    private AuthProperties authProperties;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 白名单直接放行
        if (isIgnored(path)) {
            return chain.filter(exchange);
        }

        // 提取 Bearer token
        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null
                || !authHeader.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            return unauthorized(exchange, "缺少认证凭证");
        }
        String token = authHeader.substring(BEARER_PREFIX.length()).trim();
        if (token.isEmpty()) {
            return unauthorized(exchange, "缺少认证凭证");
        }

        // 验签 + 验过期
        Claims claims;
        try {
            claims = JWTUtil.parseJWT(jwtProperty.getSecretKey(), token);
        } catch (JwtException | IllegalArgumentException e) {
            return unauthorized(exchange, "认证凭证无效或已过期，请重新登录");
        }

        // 先移除客户端可能伪造的同名请求头，再写入网关解析出的用户身份
        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_ID);
                    headers.set(HEADER_USER_ID, claims.getSubject());
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private boolean isIgnored(String path) {
        return authProperties.getIgnoreUrls().stream()
                .anyMatch(pattern -> pathMatcher.match(pattern, path));
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(Result.error(msg));
        } catch (JsonProcessingException e) {
            bytes = "{\"code\":1,\"msg\":\"认证失败\"}".getBytes(StandardCharsets.UTF_8);
        }

        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
