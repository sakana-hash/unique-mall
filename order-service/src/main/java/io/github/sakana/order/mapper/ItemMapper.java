package io.github.sakana.order.mapper;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.enumeration.OperationType;
import io.github.sakana.order.pojo.entity.Item;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ItemMapper {

    @AutoFill(OperationType.INSERT)
    int batchInsert(@Param("items") List<Item> items);
}
