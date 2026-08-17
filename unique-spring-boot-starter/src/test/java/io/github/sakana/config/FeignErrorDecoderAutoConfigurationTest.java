package io.github.sakana.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.codec.ErrorDecoder;
import io.github.sakana.feign.FeignResultErrorDecoder;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class FeignErrorDecoderAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withConfiguration(AutoConfigurations.of(
                    FeignErrorDecoderAutoConfiguration.class
            ));

    @Test
    @DisplayName("存在Jackson和Feign时自动注册错误解码器")
    void shouldAutoConfigureDecoder() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(ErrorDecoder.class);
            assertThat(context.getBean(ErrorDecoder.class))
                    .isInstanceOf(FeignResultErrorDecoder.class);
        });
    }

    @Test
    @DisplayName("应用自定义ErrorDecoder时自动配置退让")
    void shouldBackOffForCustomDecoder() {
        ErrorDecoder customDecoder = (methodKey, response) ->
                new IllegalStateException("custom");

        contextRunner
                .withBean("customErrorDecoder", ErrorDecoder.class, () -> customDecoder)
                .run(context -> {
                    assertThat(context).hasSingleBean(ErrorDecoder.class);
                    assertThat(context.getBean(ErrorDecoder.class)).isSameAs(customDecoder);
                });
    }
}
