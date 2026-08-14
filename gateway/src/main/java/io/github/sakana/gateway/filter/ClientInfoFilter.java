package io.github.sakana.gateway.filter;

import io.github.sakana.common.constant.HeadersConstant;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import ua_parser.Client;
import ua_parser.Parser;

/**
 * 客户端信息过滤器：
 * 从 nginx 设置的代理请求头（X-Real-IP / X-Forwarded-For）中解析客户端真实 IP，
 * 统一以 X-User-IP 请求头透传给下游服务。
 */
@Component
public class ClientInfoFilter implements GlobalFilter, Ordered {

    private static final String HEADER_USER_DEVICE = HeadersConstant.USER_DEVICE;
    private static final String HEADER_USER_IP = HeadersConstant.USER_IP;
    private static final Parser uaParser = new Parser();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();

        String ip = getUserIP(request.getHeaders());
        String device = getUserDevice(request.getHeaders());

        ServerHttpRequest mutatedRequest = request.mutate()
                .headers(headers -> {
                    headers.remove(HEADER_USER_DEVICE);
                    headers.remove(HEADER_USER_IP);
                    headers.set(HEADER_USER_DEVICE, device == null? "unknown": device);
                    headers.set(HEADER_USER_IP, ip == null? "unknown": ip);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutatedRequest).build());
    }

    private static String getUserDevice(HttpHeaders headers) {
        String agentHeader = headers.getFirst("User-Agent");
        if (agentHeader == null) {
            return null;
        }

        Client client = uaParser.parse(agentHeader);
        return client.device.family;
    }

    /**
     * 从 nginx 设置的请求头中解析客户端真实 IP：
     * 优先取 X-Real-IP（nginx 中显式设置为 $remote_addr，即 nginx 看到的直连客户端地址）；
     * 取不到时退化为 X-Forwarded-For 中第一个有效的 IP 段。
     * 都取不到时返回 null，由调用方兜底。
     */
    private static String getUserIP(HttpHeaders headers) {
        String ip = headers.getFirst("X-Real-IP");
        if (isValidIP(ip)) {
            return ip;
        }

        String forwardedFor = headers.getFirst("X-Forwarded-For");
        if (forwardedFor != null) {
            // 形如 "客户端, 代理1, 代理2"，取第一个非 unknown 的段
            for (String segment : forwardedFor.split(",")) {
                String candidate = segment.trim();
                if (isValidIP(candidate)) {
                    return candidate;
                }
            }
        }

        return null;
    }

    private static boolean isValidIP(String ip) {
        return ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip);
    }

    @Override
    public int getOrder() {
        return 1;
    }
}
