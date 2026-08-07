package io.github.sakana.stock.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Lock extends UpdatableEntity {

    private Long skuId;
    private Long orderId;
    private Integer quantity;
    private Integer status; // 状态 0锁定 1已扣减 2已释放
    private LocalDateTime expireTime;
}
