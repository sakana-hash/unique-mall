package io.github.sakana.user.pojo.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserProfile {

    private Long id;
    private Long userId;
    private String nickname;
    private String avatar;
    private Integer gender;
    private LocalDate birthday;

    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
