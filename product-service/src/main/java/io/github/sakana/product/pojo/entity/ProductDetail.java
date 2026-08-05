package io.github.sakana.product.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.Data;

@Data
public class ProductDetail extends UpdatableEntity {

    private Long productId;
    private String content;
}
