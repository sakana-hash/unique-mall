package io.github.sakana.stock.mapper;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.enumeration.OperationType;
import io.github.sakana.stock.pojo.entity.Record;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface RecordMapper {

    @AutoFill(OperationType.INSERT)
    int batchInsert(@Param("records") List<Record> records);
}
