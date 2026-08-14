package io.github.sakana.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关开发者模式配置。
 *
 * <p>该模式会通过网关暴露服务间调用使用的 internal 接口，生产环境必须关闭。</p>
 */
@Data
@ConfigurationProperties(prefix = "unique-mall.gateway.developer-mode")
public class DeveloperModeProperties {

    private boolean enabled;
}
