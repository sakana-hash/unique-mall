package io.github.sakana.user.mapper;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.common.enumeration.enumeration.OperationType;
import io.github.sakana.user.pojo.entity.UserProfile;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserProfileMapper {

    @Insert("insert into `user`.`user_profile` (id, user_id, created_time, updated_time) values " +
            "(#{id}, #{userId}, #{createdTime}, #{updatedTime})")
    @AutoFill(OperationType.INSERT)
    int insert(UserProfile profile);
}
