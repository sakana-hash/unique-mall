package io.github.sakana.user.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UserProfile extends UpdatableEntity {

    private Long userId;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;
}
