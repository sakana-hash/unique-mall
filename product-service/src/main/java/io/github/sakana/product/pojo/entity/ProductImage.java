package io.github.sakana.product.pojo.entity;

import io.github.sakana.common.entity.BaseEntity;
import lombok.Data;

@Data
public class ProductImage extends BaseEntity {

    private Long productId;
    private String url;
    private Integer type;
    private Integer sort;
}
