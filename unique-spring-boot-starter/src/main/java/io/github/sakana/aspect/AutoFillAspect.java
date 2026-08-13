package io.github.sakana.aspect;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.common.entity.BaseEntity;
import io.github.sakana.common.entity.UpdatableEntity;
import io.github.sakana.enumeration.OperationType;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Aspect
@Component
public class AutoFillAspect {

    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;

    @Pointcut("@annotation(io.github.sakana.annotation.AutoFill)")
    public void autoFillPointCut() {}

    @Before("autoFillPointCut()")
    public void autoFill(JoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        AutoFill autoFill = signature.getMethod().getAnnotation(AutoFill.class);
        OperationType operationType = autoFill.value();

        Object[] args = joinPoint.getArgs();
        if (args == null || args.length != 1) {
            return;
        }

        Object argument = args[0];
        LocalDateTime now = LocalDateTime.now();
        if (argument instanceof List<?> entities) {
            entities.forEach(entity -> fillEntity(entity, operationType, now));
            return;
        }

        fillEntity(argument, operationType, now);
    }

    private void fillEntity(Object argument, OperationType operationType, LocalDateTime now) {
        if (argument instanceof UpdatableEntity entity) {
            if (operationType == OperationType.INSERT) {
                if (entity.getId() == null) {
                    entity.setId(snowflakeIdGenerator.nextId());
                }
                entity.setCreatedTime(now);
                entity.setUpdatedTime(now);
            } else if (operationType == OperationType.UPDATE) {
                entity.setUpdatedTime(now);
            }
        } else if (argument instanceof BaseEntity entity) {
            if (operationType == OperationType.INSERT) {
                if (entity.getId() == null) {
                    entity.setId(snowflakeIdGenerator.nextId());
                }
                entity.setCreatedTime(now);
            }
        }
    }
}
