package io.github.sakana.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import io.github.sakana.feign.FeignResultErrorDecoder;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.context.annotation.Bean;

@AutoConfiguration(after = JacksonAutoConfiguration.class)
@ConditionalOnClass(ErrorDecoder.class)
@ConditionalOnBean(ObjectMapper.class)
public class FeignErrorDecoderAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(ErrorDecoder.class)
    public ErrorDecoder feignResultErrorDecoder(ObjectMapper objectMapper) {
        return new FeignResultErrorDecoder(objectMapper);
    }
}
