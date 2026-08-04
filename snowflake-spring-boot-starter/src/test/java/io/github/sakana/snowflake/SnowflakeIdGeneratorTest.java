package io.github.sakana.snowflake;

import io.github.sakana.snowflake.properties.SnowflakeProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

/**
 * SnowflakeIdGenerator 单元测试。
 *
 * <p>雪花ID位布局（共64位，最高位为符号位恒为0）：
 * <pre>
 * | 1bit 符号位 | 41bit 时间戳 | 5bit 数据中心ID | 5bit 机器ID | 12bit 序列号 |
 * </pre>
 */
@DisplayName("SnowflakeIdGenerator 单元测试")
class SnowflakeIdGeneratorTest {

    private static final long DEFAULT_EPOCH = 1577808000000L; // 2020-01-01 00:00:00

    private static final long SEQUENCE_BITS = 12L;
    private static final long WORKER_ID_BITS = 5L;
    private static final long DATACENTER_ID_BITS = 5L;

    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);              // 4095
    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;                       // 12
    private static final long DATACENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;  // 17
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATACENTER_ID_BITS; // 22

    private SnowflakeIdGenerator newGenerator(long datacenterId, long workerId) {
        return new SnowflakeIdGenerator(datacenterId, workerId, DEFAULT_EPOCH, true, 50L);
    }

    private static long extractTimestamp(long id) {
        return (id >> TIMESTAMP_SHIFT);
    }

    private static long extractDatacenterId(long id) {
        return (id >> DATACENTER_ID_SHIFT) & ~(-1L << DATACENTER_ID_BITS);
    }

    private static long extractWorkerId(long id) {
        return (id >> WORKER_ID_SHIFT) & ~(-1L << WORKER_ID_BITS);
    }

    private static long extractSequence(long id) {
        return id & SEQUENCE_MASK;
    }

    @Nested
    @DisplayName("构造函数参数校验")
    class ConstructorValidation {

        @ParameterizedTest(name = "非法数据中心ID: {0}")
        @ValueSource(longs = {-1L, 32L, 33L, 1024L, Long.MAX_VALUE})
        @DisplayName("数据中心ID超出 [0, 31] 范围时抛出 IllegalArgumentException")
        void shouldRejectInvalidDatacenterId(long datacenterId) {
            assertThatThrownBy(() -> newGenerator(datacenterId, 0L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("数据中心ID必须在0和31之间");
        }

        @ParameterizedTest(name = "非法机器ID: {0}")
        @ValueSource(longs = {-1L, 32L, 33L, 1024L, Long.MAX_VALUE})
        @DisplayName("机器ID超出 [0, 31] 范围时抛出 IllegalArgumentException")
        void shouldRejectInvalidWorkerId(long workerId) {
            assertThatThrownBy(() -> newGenerator(0L, workerId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("机器ID必须在0和31之间");
        }

        @ParameterizedTest(name = "datacenterId={0}, workerId={1}")
        @CsvSource({"0,0", "0,31", "31,0", "31,31", "15,16"})
        @DisplayName("边界值 datacenterId/workerId 在 [0, 31] 内时构造成功")
        void shouldAcceptBoundaryIds(long datacenterId, long workerId) {
            assertThatCode(() -> newGenerator(datacenterId, workerId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("通过 SnowflakeProperties 构造生成器")
        void shouldConstructFromProperties() {
            SnowflakeProperties properties = new SnowflakeProperties();
            properties.setDatacenterId(3L);
            properties.setWorkerId(7L);
            properties.setEpoch(DEFAULT_EPOCH);
            properties.setEnableClockBackward(true);
            properties.setMaxClockBackwardMs(50L);

            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(properties);

            long id = generator.nextId();
            assertThat(extractDatacenterId(id)).isEqualTo(3L);
            assertThat(extractWorkerId(id)).isEqualTo(7L);
        }

        @Test
        @DisplayName("使用默认 SnowflakeProperties 构造生成器")
        void shouldConstructFromDefaultProperties() {
            SnowflakeProperties properties = new SnowflakeProperties();

            SnowflakeIdGenerator generator = new SnowflakeIdGenerator(properties);

            long id = generator.nextId();
            assertThat(id).isPositive();
            assertThat(extractDatacenterId(id)).isZero();
            assertThat(extractWorkerId(id)).isZero();
        }
    }

    @Nested
    @DisplayName("ID位结构")
    class IdStructure {

        @ParameterizedTest(name = "datacenterId={0}, workerId={1}")
        @CsvSource({"0,0", "1,2", "17,9", "31,31"})
        @DisplayName("生成的ID能正确反解出数据中心ID与机器ID")
        void shouldEmbedDatacenterAndWorkerId(long datacenterId, long workerId) {
            SnowflakeIdGenerator generator = newGenerator(datacenterId, workerId);

            long id = generator.nextId();

            assertThat(extractDatacenterId(id)).isEqualTo(datacenterId);
            assertThat(extractWorkerId(id)).isEqualTo(workerId);
        }

        @Test
        @DisplayName("生成的ID能反解出接近当前时刻的时间戳")
        void shouldEmbedReasonableTimestamp() {
            SnowflakeIdGenerator generator = newGenerator(1L, 1L);

            long before = System.currentTimeMillis();
            long id = generator.nextId();
            long after = System.currentTimeMillis();

            long timestamp = extractTimestamp(id) + DEFAULT_EPOCH;
            assertThat(timestamp).isBetween(before, after);
        }

        @Test
        @DisplayName("生成的ID为正数且序列号在 [0, 4095] 范围内")
        void shouldGeneratePositiveIdWithValidSequence() {
            SnowflakeIdGenerator generator = newGenerator(2L, 3L);

            for (int i = 0; i < 1000; i++) {
                long id = generator.nextId();
                assertThat(id).isPositive();
                assertThat(extractSequence(id)).isBetween(0L, SEQUENCE_MASK);
            }
        }

        @Test
        @DisplayName("首个ID的序列号从0开始")
        void shouldStartSequenceAtZero() {
            SnowflakeIdGenerator generator = newGenerator(5L, 6L);

            long id = generator.nextId();

            assertThat(extractSequence(id)).isZero();
        }

        @Test
        @DisplayName("不同机器ID的生成器在同一毫秒产生的ID不同")
        void shouldDistinguishIdsFromDifferentWorkers() {
            SnowflakeIdGenerator generatorA = newGenerator(1L, 1L);
            SnowflakeIdGenerator generatorB = newGenerator(1L, 2L);

            long idA = generatorA.nextId();
            long idB = generatorB.nextId();

            assertThat(idA).isNotEqualTo(idB);
        }
    }

    @Nested
    @DisplayName("唯一性与单调性")
    class UniquenessAndMonotonicity {

        @Test
        @DisplayName("单线程生成10万个ID全部唯一")
        void shouldGenerateUniqueIds() {
            SnowflakeIdGenerator generator = newGenerator(1L, 1L);
            int count = 100_000;

            Set<Long> ids = new HashSet<>(count * 2);
            for (int i = 0; i < count; i++) {
                ids.add(generator.nextId());
            }

            assertThat(ids).hasSize(count);
        }

        @Test
        @DisplayName("单线程生成的ID严格单调递增")
        void shouldGenerateMonotonicallyIncreasingIds() {
            SnowflakeIdGenerator generator = newGenerator(1L, 1L);
            int count = 50_000;

            long previous = -1L;
            for (int i = 0; i < count; i++) {
                long id = generator.nextId();
                assertThat(id).isGreaterThan(previous);
                previous = id;
            }
        }

        @Test
        @DisplayName("跨毫秒生成ID时时间戳部分随时间推进")
        void shouldAdvanceTimestampAcrossMilliseconds() throws InterruptedException {
            SnowflakeIdGenerator generator = newGenerator(1L, 1L);

            long first = generator.nextId();
            Thread.sleep(5L);
            long second = generator.nextId();

            assertThat(extractTimestamp(second)).isGreaterThan(extractTimestamp(first));
        }
    }

    @Nested
    @DisplayName("时钟回拨处理")
    class ClockBackward {

        /** 通过反射将 lastTimestamp 设置为未来时间，模拟时钟回拨。 */
        private void rewindClock(SnowflakeIdGenerator generator, long futureTimestamp) {
            ReflectionTestUtils.setField(generator, "lastTimestamp", futureTimestamp);
        }

        @Test
        @DisplayName("未启用时钟回拨容忍时，检测到回拨抛出 IllegalStateException")
        void shouldThrowWhenClockBackwardAndToleranceDisabled() {
            SnowflakeIdGenerator generator =
                    new SnowflakeIdGenerator(1L, 1L, DEFAULT_EPOCH, false, 50L);
            rewindClock(generator, System.currentTimeMillis() + 100L);

            assertThatThrownBy(generator::nextId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("检查到时钟回拨");
        }

        @Test
        @DisplayName("enableClockBackward=true 但 maxClockBackwardMs<=0 时仍视为未启用容忍")
        void shouldThrowWhenToleranceMsIsZero() {
            SnowflakeIdGenerator generator =
                    new SnowflakeIdGenerator(1L, 1L, DEFAULT_EPOCH, true, 0L);
            rewindClock(generator, System.currentTimeMillis() + 10L);

            assertThatThrownBy(generator::nextId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("检查到时钟回拨");
        }

        @Test
        @DisplayName("回拨幅度超过容忍上限时抛出 IllegalStateException")
        void shouldThrowWhenOffsetExceedsTolerance() {
            long maxClockBackwardMs = 50L;
            SnowflakeIdGenerator generator =
                    new SnowflakeIdGenerator(1L, 1L, DEFAULT_EPOCH, true, maxClockBackwardMs);
            rewindClock(generator, System.currentTimeMillis() + maxClockBackwardMs + 200L);

            assertThatThrownBy(generator::nextId)
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("时钟回拨超过容忍范围");
        }

        @Test
        @DisplayName("回拨幅度在容忍范围内时，等待追上上次时间戳后正常生成ID")
        void shouldRecoverWhenOffsetWithinTolerance() {
            long maxClockBackwardMs = 200L;
            SnowflakeIdGenerator generator =
                    new SnowflakeIdGenerator(1L, 1L, DEFAULT_EPOCH, true, maxClockBackwardMs);
            long futureTimestamp = System.currentTimeMillis() + 20L;
            rewindClock(generator, futureTimestamp);

            long id = generator.nextId();

            assertThat(id).isPositive();
            // 恢复后使用的时间戳必须不小于上次时间戳，保证ID时间维度不回退
            assertThat(extractTimestamp(id) + DEFAULT_EPOCH).isGreaterThanOrEqualTo(futureTimestamp);
        }

        @Test
        @DisplayName("容忍恢复后继续生成的ID保持唯一且递增")
        void shouldKeepUniqueAfterRecovery() {
            SnowflakeIdGenerator generator =
                    new SnowflakeIdGenerator(1L, 1L, DEFAULT_EPOCH, true, 200L);
            long recovered = generator.nextId();
            rewindClock(generator, System.currentTimeMillis() + 10L);
            long afterRewind = generator.nextId();

            List<Long> ids = new ArrayList<>();
            ids.add(recovered);
            ids.add(afterRewind);
            for (int i = 0; i < 1000; i++) {
                ids.add(generator.nextId());
            }

            assertThat(new HashSet<>(ids)).hasSize(ids.size());
            assertThat(afterRewind).isGreaterThan(recovered);
        }
    }

    @Nested
    @DisplayName("并发安全")
    class Concurrency {

        @Test
        @DisplayName("多线程并发生成的ID全部唯一")
        void shouldGenerateUniqueIdsUnderConcurrency() throws InterruptedException {
            SnowflakeIdGenerator generator = newGenerator(1L, 1L);
            int threadCount = 8;
            int idsPerThread = 10_000;
            Set<Long> ids = new ConcurrentSkipListSet<>();
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch doneLatch = new CountDownLatch(threadCount);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            try {
                for (int t = 0; t < threadCount; t++) {
                    executor.submit(() -> {
                        try {
                            startLatch.await();
                            for (int i = 0; i < idsPerThread; i++) {
                                ids.add(generator.nextId());
                            }
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }
                startLatch.countDown();
                assertThat(doneLatch.await(60L, TimeUnit.SECONDS)).isTrue();
            } finally {
                executor.shutdownNow();
            }

            assertThat(ids).hasSize(threadCount * idsPerThread);
        }

        @Test
        @DisplayName("并发高峰下序列号耗尽时自动切换到下一毫秒，不产生重复")
        void shouldNotDuplicateWhenSequenceExhausted() {
            SnowflakeIdGenerator generator = newGenerator(1L, 1L);
            // 短时间内高速生成远超单毫秒序列容量(4096)的ID，触发序列耗尽等待下一毫秒的逻辑
            int count = 50_000;

            Set<Long> ids = new HashSet<>(count * 2);
            for (int i = 0; i < count; i++) {
                ids.add(generator.nextId());
            }

            assertThat(ids).hasSize(count);
        }
    }
}
