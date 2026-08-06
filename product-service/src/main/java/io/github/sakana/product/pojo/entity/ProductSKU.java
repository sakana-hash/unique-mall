package io.github.sakana.product.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import io.github.sakana.product.pojo.vo.ProductSKUVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class ProductSKU extends UpdatableEntity {

    private Long productId;
    private String skuCode;
    private Long price;
    private Integer status;
    private Integer productStatus;  // 冗余商品上下架状态，与 product.status 保持一致

    public ProductSKUVO toVO() {
        ProductSKUVO vo = new ProductSKUVO();
        BeanUtils.copyProperties(this, vo);
        return vo;
    }
}
