package io.github.sakana.user.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User extends UpdatableEntity {

    private String username;
    private String password;
    private String phone;
    private String email;
    private Integer status;
}
