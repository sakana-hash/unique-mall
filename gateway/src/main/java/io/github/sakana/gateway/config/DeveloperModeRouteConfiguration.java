package io.github.sakana.gateway.config;

import io.github.sakana.gateway.properties.DeveloperModeProperties;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 开发者模式专用路由。
 *
 * <p>仅控制 internal 接口能否从网关路由，接口仍会经过全局鉴权过滤器。</p>
 */
@Configuration
public class DeveloperModeRouteConfiguration {

    @Bean
    public RouteLocator developerInternalRoutes(
            RouteLocatorBuilder builder,
            DeveloperModeProperties properties
    ) {
        RouteLocatorBuilder.Builder routes = builder.routes();
        if (!properties.isEnabled()) {
            return routes.build();
        }

        return routes
                .route("developer-user-internal", route -> route
                        .path("/internal/user/**")
                        .uri("lb://user-service"))
                .route("developer-product-internal", route -> route
                        .path("/internal/product/**")
                        .uri("lb://product-service"))
                .route("developer-stock-internal", route -> route
                        .path("/internal/stock/**")
                        .uri("lb://stock-service"))
                .build();
    }
}
