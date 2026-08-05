package io.github.sakana.common.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdatableEntity extends BaseEntity {

    private LocalDateTime updatedTime;
}
