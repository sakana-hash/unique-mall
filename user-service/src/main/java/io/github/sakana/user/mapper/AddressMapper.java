package io.github.sakana.user.mapper;

import io.github.sakana.user.pojo.entity.Address;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AddressMapper {

    @Select("""
    select *
    from user.user_address
    where id = #{id}
        and user_id = #{userId}
""")
    Address selectByIdAndUserId(@Param("id") Long id, @Param("userId") Long userId);
}
