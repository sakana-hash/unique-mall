package io.github.sakana.order.mapper;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.enumeration.OperationType;
import io.github.sakana.order.pojo.entity.Address;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AddressMapper {

    @AutoFill(OperationType.INSERT)
    int insert(Address address);
}
