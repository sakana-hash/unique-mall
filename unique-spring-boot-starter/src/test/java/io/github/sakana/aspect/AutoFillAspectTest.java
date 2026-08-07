package io.github.sakana.aspect;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.common.entity.BaseEntity;
import io.github.sakana.common.entity.UpdatableEntity;
import io.github.sakana.common.enumeration.OperationType;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AutoFillAspectTest {

    private final SnowflakeIdGenerator idGenerator =
            new SnowflakeIdGenerator(1, 1, 1577808000000L, true, 5L);
    private final AutoFillAspect aspect = new AutoFillAspect();

    AutoFillAspectTest() {
        ReflectionTestUtils.setField(aspect, "snowflakeIdGenerator", idGenerator);
    }

    @Test
    void shouldFillEveryEntityWhenBatchInserting() throws Exception {
        UpdatableEntity first = new UpdatableEntity();
        UpdatableEntity second = new UpdatableEntity();
        second.setId(99L);

        aspect.autoFill(joinPoint("batchInsert", List.class, List.of(first, second)));

        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isEqualTo(99L);
        assertThat(first.getCreatedTime()).isNotNull();
        assertThat(first.getUpdatedTime()).isEqualTo(first.getCreatedTime());
        assertThat(second.getCreatedTime()).isEqualTo(first.getCreatedTime());
        assertThat(second.getUpdatedTime()).isEqualTo(first.getUpdatedTime());
    }

    @Test
    void shouldFillBaseEntitiesAndIgnoreNullOrNonEntityItems() throws Exception {
        BaseEntity first = new BaseEntity();
        BaseEntity second = new BaseEntity();

        aspect.autoFill(joinPoint(
                "batchInsert",
                List.class,
                Arrays.asList(first, null, "not an entity", second)
        ));

        assertThat(first.getId()).isNotNull();
        assertThat(second.getId()).isNotNull().isNotEqualTo(first.getId());
        assertThat(first.getCreatedTime()).isNotNull();
        assertThat(second.getCreatedTime()).isEqualTo(first.getCreatedTime());
    }

    @Test
    void shouldUpdateEveryEntityWhenBatchUpdating() throws Exception {
        UpdatableEntity first = new UpdatableEntity();
        UpdatableEntity second = new UpdatableEntity();

        aspect.autoFill(joinPoint("batchUpdate", List.class, List.of(first, second)));

        assertThat(first.getCreatedTime()).isNull();
        assertThat(second.getCreatedTime()).isNull();
        assertThat(first.getUpdatedTime()).isNotNull();
        assertThat(second.getUpdatedTime()).isEqualTo(first.getUpdatedTime());
    }

    private JoinPoint joinPoint(String methodName, Class<?> parameterType, Object argument) throws Exception {
        Method method = MapperMethods.class.getDeclaredMethod(methodName, parameterType);
        MethodSignature signature = proxy(MethodSignature.class, invokedMethod ->
                invokedMethod.getName().equals("getMethod") ? method : null);

        return proxy(JoinPoint.class, invokedMethod -> switch (invokedMethod.getName()) {
            case "getSignature" -> signature;
            case "getArgs" -> new Object[]{argument};
            default -> null;
        });
    }

    private <T> T proxy(Class<T> type, MethodHandler handler) {
        return type.cast(Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> handler.invoke(method)
        ));
    }

    private interface MethodHandler {
        Object invoke(Method method);
    }

    private interface MapperMethods {

        @AutoFill(OperationType.INSERT)
        void batchInsert(List<?> entities);

        @AutoFill(OperationType.UPDATE)
        void batchUpdate(List<?> entities);
    }
}
