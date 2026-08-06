package io.github.sakana.mockdata.pojo;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类，对应 product.product_category 表
 * （product-service 中没有该表的实体类，故在工具内定义）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductCategory extends UpdatableEntity {

    /** 父分类ID，一级分类为 0 */
    private Long parentId;

    /** 分类名称 */
    private String name;

    /** 层级：1 一级分类，2 二级分类 */
    private Integer level;

    /** 状态：0 不可见，1 可见 */
    private Integer status;
}
