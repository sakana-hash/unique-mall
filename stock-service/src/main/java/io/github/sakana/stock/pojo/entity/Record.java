package io.github.sakana.stock.pojo.entity;

import io.github.sakana.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Record extends BaseEntity {

    private Long skuId;
    private Long orderId;
    private Integer changeType; // 流水类型 1初始化库存 2锁定库存 3扣减库存 4释放库存 5人工调整
    private Integer changeAmount;
    private Integer beforeStock;
    private Integer afterStock;
}
