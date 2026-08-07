package io.github.sakana.stock.mapper;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.common.enumeration.OperationType;
import io.github.sakana.stock.pojo.entity.Lock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface LockMapper {

    @AutoFill(OperationType.INSERT)
    int batchInsert(@Param("locks") List<Lock> locks);
}
