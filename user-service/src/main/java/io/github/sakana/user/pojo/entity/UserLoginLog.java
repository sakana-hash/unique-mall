package io.github.sakana.user.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UserLoginLog {

    private Long id;
    private Long userId;
    private String ip;
    private String device;
    private LocalDateTime loginTime;
    private LocalDateTime createdTime;
}
