package io.github.sakana.user.mapper;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.common.enumeration.enumeration.OperationType;
import io.github.sakana.user.pojo.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {

    @Insert("insert into `user`.`user` (id, username, password, created_time, updated_time) values " +
            "(#{id}, #{username}, #{password}, #{createdTime}, #{updatedTime})")
    @AutoFill(OperationType.INSERT)
    int insert(User user);

    @Select("select * from `user`.`user` where username = #{username}")
    User selectByUsername(String username);
}
