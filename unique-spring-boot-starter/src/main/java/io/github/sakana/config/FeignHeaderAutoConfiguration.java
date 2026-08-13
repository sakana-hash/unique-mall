package io.github.sakana.config;

import feign.RequestInterceptor;
import io.github.sakana.interceptor.FeignHeaderInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@ConditionalOnClass(RequestInterceptor.class)
public class FeignHeaderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "feignHeaderInterceptor")
    public RequestInterceptor feignHeaderInterceptor() {
        return new FeignHeaderInterceptor();
    }
}
