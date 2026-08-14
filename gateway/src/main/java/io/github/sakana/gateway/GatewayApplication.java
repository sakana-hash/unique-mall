package io.github.sakana.gateway;

import io.github.sakana.common.properties.JWTProperty;
import io.github.sakana.gateway.properties.AuthProperties;
import io.github.sakana.gateway.properties.DeveloperModeProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        JWTProperty.class,
        AuthProperties.class,
        DeveloperModeProperties.class
})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
