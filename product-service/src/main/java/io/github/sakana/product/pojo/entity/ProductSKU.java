package io.github.sakana.product.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.Data;

@Data
public class ProductSKU extends UpdatableEntity {

    private Long productId;
    private String skuCode;
    private Long price;
    private Integer status;
    private Integer productStatus;  // 冗余商品上下架状态，与 product.status 保持一致
}
