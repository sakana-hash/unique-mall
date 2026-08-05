package io.github.sakana.user.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.Data;

@Data
public class User extends UpdatableEntity {

    private String username;
    private String password;
    private String phone;
    private String email;
    private Integer status;
}
