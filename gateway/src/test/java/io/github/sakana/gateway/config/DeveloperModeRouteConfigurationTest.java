package io.github.sakana.gateway.config;

import io.github.sakana.gateway.properties.DeveloperModeProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.handler.predicate.PathRoutePredicateFactory;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DeveloperModeRouteConfigurationTest {

    private final DeveloperModeRouteConfiguration configuration =
            new DeveloperModeRouteConfiguration();

    @Test
    @DisplayName("开发者模式关闭时不创建internal路由")
    void shouldNotExposeInternalRoutesWhenDisabled() {
        DeveloperModeProperties properties = new DeveloperModeProperties();
        properties.setEnabled(false);

        List<Route> routes = routes(properties);

        assertTrue(routes.isEmpty());
    }

    @Test
    @DisplayName("开发者模式开启时创建三个服务的internal路由")
    void shouldExposeInternalRoutesWhenEnabled() {
        DeveloperModeProperties properties = new DeveloperModeProperties();
        properties.setEnabled(true);

        Map<String, Route> routes = routes(properties).stream()
                .collect(Collectors.toMap(Route::getId, Function.identity()));

        assertEquals(3, routes.size());
        assertEquals(URI.create("lb://user-service"),
                routes.get("developer-user-internal").getUri());
        assertEquals(URI.create("lb://product-service"),
                routes.get("developer-product-internal").getUri());
        assertEquals(URI.create("lb://stock-service"),
                routes.get("developer-stock-internal").getUri());
    }

    @Test
    @DisplayName("开发者路由只匹配各服务对应的internal路径")
    void shouldMatchOnlyExpectedInternalPaths() {
        DeveloperModeProperties properties = new DeveloperModeProperties();
        properties.setEnabled(true);
        Map<String, Route> routes = routes(properties).stream()
                .collect(Collectors.toMap(Route::getId, Function.identity()));

        assertTrue(matches(routes.get("developer-user-internal"),
                "/internal/user/address/1001"));
        assertTrue(matches(routes.get("developer-product-internal"),
                "/internal/product/sku/trade-info"));
        assertTrue(matches(routes.get("developer-stock-internal"),
                "/internal/stock/lock"));

        assertFalse(matches(routes.get("developer-user-internal"),
                "/internal/stock/lock"));
        assertFalse(matches(routes.get("developer-product-internal"),
                "/api/product/1001"));
    }

    private List<Route> routes(DeveloperModeProperties properties) {
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.registerBean(PathRoutePredicateFactory.class);
            context.refresh();
            RouteLocator locator = configuration.developerInternalRoutes(
                    new RouteLocatorBuilder(context),
                    properties
            );
            return locator.getRoutes().collectList().block();
        }
    }

    private static boolean matches(Route route, String path) {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get(path).build()
        );
        return Boolean.TRUE.equals(Mono.from(route.getPredicate().apply(exchange)).block());
    }
}
