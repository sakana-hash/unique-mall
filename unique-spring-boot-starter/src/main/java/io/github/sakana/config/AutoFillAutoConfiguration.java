package io.github.sakana.config;

import io.github.sakana.aspect.AutoFillAspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
public class AutoFillAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AutoFillAspect autoFillAspect() {
        return new AutoFillAspect();
    }
}
