package io.github.sakana.common.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "unique-mall.jwt")
public class JWTProperty {

    private String secretKey;
    private long ttl;
}
