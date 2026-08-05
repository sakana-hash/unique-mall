package io.github.sakana.product.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.Data;

@Data
public class Product extends UpdatableEntity {

    private Long categoryId;
    private String name;
    private String subtitle;
    private String brand;
    private Integer status;
}
