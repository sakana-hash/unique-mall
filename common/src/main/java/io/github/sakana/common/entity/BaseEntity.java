package io.github.sakana.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class BaseEntity {

    private Long id;
    private LocalDateTime createdTime;
}
