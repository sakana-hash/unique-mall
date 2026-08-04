package io.github.sakana.user.pojo.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {

    private Long id;
    private String username;
    private String password;
    private String phone;
    private String email;
    private Integer status;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
