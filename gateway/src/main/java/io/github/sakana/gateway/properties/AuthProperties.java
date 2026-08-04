package io.github.sakana.gateway.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关鉴权配置
 */
@Data
@ConfigurationProperties(prefix = "unique-mall.gateway.auth")
public class AuthProperties {

    /**
     * 免登录白名单 URL 列表，支持 Ant 风格路径匹配，如 /api/user/auth/**
     */
    private List<String> ignoreUrls = new ArrayList<>();
}
