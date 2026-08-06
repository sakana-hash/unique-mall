package io.github.sakana.mockdata;

import io.github.sakana.mockdata.config.MockDataProperties;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Mock 数据批量插入工具入口。
 *
 * <p>基于 product-service 的建表脚本与实体类，使用 unique-spring-boot-starter 中的
 * {@link io.github.sakana.snowflake.SnowflakeIdGenerator} 生成主键，
 * 通过多线程 + 多行 VALUES 批量 INSERT 向 product 库灌入
 * 约 100 万商品、300 万 SKU（以及 1:1 的商品详情、每商品 2~4 张图片）。</p>
 */
@SpringBootApplication
@MapperScan("io.github.sakana.mockdata.mapper")
@EnableConfigurationProperties(MockDataProperties.class)
public class MockDataApplication {

    public static void main(String[] args) {
        new SpringApplicationBuilder(MockDataApplication.class)
                // 使用独立配置文件名 mock-data.yaml，避免加载到 product-service jar 内的 application.yaml
                .properties("spring.config.name=mock-data")
                .run(args);
    }
}
