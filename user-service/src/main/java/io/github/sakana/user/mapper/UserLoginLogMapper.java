package io.github.sakana.user.mapper;

import io.github.sakana.annotation.AutoFill;
import io.github.sakana.enumeration.OperationType;
import io.github.sakana.user.pojo.entity.UserLoginLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserLoginLogMapper {

    @Insert("insert into `user`.`user_login_log` (id, user_id, ip, device, login_time, created_time) " +
            "values (#{id}, #{userId}, #{ip}, #{device}, #{loginTime}, #{createdTime})")
    @AutoFill(OperationType.INSERT)
    int insert(UserLoginLog loginLog);
}
