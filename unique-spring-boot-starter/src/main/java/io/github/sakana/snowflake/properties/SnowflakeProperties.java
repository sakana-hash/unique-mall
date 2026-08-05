package io.github.sakana.snowflake.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "unique-mall.snowflake")
public class SnowflakeProperties {

    private long datacenterId;  // 数据中心ID，范围0-31
    private long workerId;  // 机器ID，范围0-31
    private long epoch = 1577808000000L;    // 起始时间戳（毫秒），默认2020-01-01 00:00:00
    private boolean enableClockBackward = true;  // 是否启用时钟回拨容忍功能
    private long maxClockBackwardMs = 50; // 最大时钟回拨容忍时间（毫秒）
}
