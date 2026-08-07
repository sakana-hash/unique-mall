package io.github.sakana.stock.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Lock extends UpdatableEntity {

    private Long skuId;
    private Long orderId;
    private Integer quantity;
    private Integer status; // 状态 0锁定 1已扣减 2已释放
    private LocalDateTime expireTime;
}
