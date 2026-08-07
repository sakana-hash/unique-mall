package io.github.sakana.stock.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import io.github.sakana.stock.enumeration.StockStatus;
import lombok.Data;

@Data
public class Stock extends UpdatableEntity {

    private Long skuId;
    private Integer totalStock;
    private Integer availableStock;
    private Integer lockedStock;
    private Integer version;

    private StockStatus status;
}
