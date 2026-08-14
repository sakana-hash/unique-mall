package io.github.sakana.order.pojo.entity;

import io.github.sakana.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Item extends BaseEntity {

    private Long orderId;
    private Long skuId;
    private Long productId;
    private String productName;
    private String skuCode;
    private String imageUrl;
    private Long price;
    private Integer quantity;
    private Long amount;
}
