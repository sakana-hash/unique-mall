package io.github.sakana.snowflake;

import io.github.sakana.snowflake.properties.SnowflakeProperties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SnowflakeIdGenerator {

    private static final long DATACENTER_ID_BITS = 5L;  // 数据中心ID位数
    private static final long WORKER_ID_BITS = 5L;  // 机器ID位数
    private static final long SEQUENCE_BITS = 12L;  // 序列号位数

    private static final long MAX_DATACENTER_ID = ~(-1L << DATACENTER_ID_BITS); // 最大数据中心ID
    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS); // 最大机器ID
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;  // 机器ID左移位数
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS; // 数据中心ID左移位数
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS;    // 时间戳左移位数

    private final long datacenterId;
    private final long workerId;
    private final long epoch;
    private final boolean enableClockBackward;
    private final long maxClockBackwardMs;

    private long sequence = 0L;
    private long lastTimestamp = -1L;

    public SnowflakeIdGenerator(long datacenterId, long workerId, long epoch,
                                boolean enableClockBackward, long maxClockBackwardMs) {
        if (datacenterId > MAX_DATACENTER_ID || datacenterId < 0) {
            throw new IllegalArgumentException(String.format(
                    "数据中心ID必须在0和%d之间", MAX_DATACENTER_ID));
        }
        if (workerId > MAX_WORKER_ID || workerId < 0) {
            throw new IllegalArgumentException(String.format(
                    "机器ID必须在0和%d之间", MAX_WORKER_ID));
        }

        this.datacenterId = datacenterId;
        this.workerId = workerId;
        this.epoch = epoch;
        this.enableClockBackward = maxClockBackwardMs > 0 && enableClockBackward;
        this.maxClockBackwardMs = maxClockBackwardMs;

        log.info("雪花ID生成器初始化完成: 数据中心ID={}, 机器ID={}, 起始时间戳={}",
                datacenterId, workerId, epoch);
    }

    public SnowflakeIdGenerator(SnowflakeProperties properties) {
        this(properties.getDatacenterId(),
                properties.getWorkerId(),
                properties.getEpoch(),
                properties.isEnableClockBackward(),
                properties.getMaxClockBackwardMs());
    }

    public synchronized long nextId() {
        long timestamp = System.currentTimeMillis();

        // 时钟回拨检测
        if (timestamp < lastTimestamp) {
            long offset = lastTimestamp - timestamp;
            if (!enableClockBackward) {
                throw new IllegalStateException(String.format(
                        "检查到时钟回拨 %dms, 请检查系统时间或启用时钟回拨容忍", offset
                ));
            }
            if (offset > maxClockBackwardMs) {
                throw new IllegalStateException(String.format(
                        "时钟回拨超过容忍范围: 当前时间戳=%d, 上次时间戳=%d, 偏移量=%dms, 允许最大偏移量=%dms",
                        timestamp, lastTimestamp, offset, maxClockBackwardMs
                ));
            }

            log.warn("检测到时钟回拨 {}ms", offset);
            timestamp = tillNextMillis(lastTimestamp);
        }

        // 判断当前时间戳和上次获取的时间戳是否在同一毫秒内
        if (timestamp == lastTimestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                // 在同一毫秒内但 sequence 已用尽, 等待到下一毫秒
                timestamp = tillNextMillis(lastTimestamp);
            }
        } else {
            // 不在同一毫秒内, 重置 sequence
            sequence = 0L;
        }

        lastTimestamp = timestamp;
        return ((timestamp - epoch) << TIMESTAMP_SHIFT) |
                (datacenterId << DATACENTER_ID_SHIFT) |
                (workerId << WORKER_ID_SHIFT) |
                sequence;
    }

    private long tillNextMillis(long lastTimestamp) {
        long timestamp = System.currentTimeMillis();
        while (timestamp <= lastTimestamp) {
            timestamp = System.currentTimeMillis();
        }
        return timestamp;
    }
}
