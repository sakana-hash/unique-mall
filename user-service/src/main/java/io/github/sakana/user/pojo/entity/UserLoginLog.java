package io.github.sakana.user.pojo.entity;

import io.github.sakana.common.entity.BaseEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLoginLog extends BaseEntity {

    private Long userId;
    private String ip;
    private String device;
    private LocalDateTime loginTime;
}
