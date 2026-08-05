package io.github.sakana.aspect;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.common.entity.BaseEntity;
import io.github.sakana.common.entity.UpdatableEntity;
import io.github.sakana.common.enumeration.enumeration.OperationType;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

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

        LocalDateTime now = LocalDateTime.now();
        if (args[0] instanceof UpdatableEntity) {
            UpdatableEntity entity = (UpdatableEntity) args[0];
            if (operationType == OperationType.INSERT) {
                if (entity.getId() == null) {
                    entity.setId(snowflakeIdGenerator.nextId());
                }
                entity.setCreatedTime(now);
                entity.setUpdatedTime(now);
            } else if (operationType == OperationType.UPDATE) {
                entity.setUpdatedTime(now);
            }
        } else if (args[0] instanceof BaseEntity) {
            BaseEntity entity = (BaseEntity) args[0];
            if (operationType == OperationType.INSERT) {
                if (entity.getId() == null) {
                    entity.setId(snowflakeIdGenerator.nextId());
                }
                entity.setCreatedTime(now);
            }
        }
    }
}
