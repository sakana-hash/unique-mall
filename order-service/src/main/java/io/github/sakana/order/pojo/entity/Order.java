package io.github.sakana.order.pojo.entity;

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
public class Order extends UpdatableEntity {

    private String orderNo;

    private Long userId;
    private Long totalAmount;
    private Long payAmount;
    private Integer status;
    private String remark;

    private LocalDateTime expireTime;
    private LocalDateTime paidTime;
    private LocalDateTime cancelledTime;
    private LocalDateTime completedTime;
}
