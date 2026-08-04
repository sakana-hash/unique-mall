package io.github.sakana.user.mapper;

import io.github.sakana.user.pojo.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper {

    @Insert("insert into `user`.`user` (id, username, password) values " +
            "(#{id}, #{username}, #{password})")
    int insert(User user);
}
