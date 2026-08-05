package io.github.sakana.snowflake;

import io.github.sakana.snowflake.properties.SnowflakeProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(SnowflakeProperties.class)
@ConditionalOnProperty(prefix = "unique-mall.snowflake", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SnowflakeIdAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public SnowflakeIdGenerator snowflakeIdGenerator(@Autowired SnowflakeProperties properties) {
        return new SnowflakeIdGenerator(properties);
    }
}
