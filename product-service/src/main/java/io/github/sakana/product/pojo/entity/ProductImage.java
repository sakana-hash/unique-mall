package io.github.sakana.product.pojo.entity;

import io.github.sakana.common.entity.BaseEntity;
import io.github.sakana.product.pojo.vo.ProductImageVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

@Data
public class ProductImage extends BaseEntity {

    private Long productId;
    private String url;
    private Integer type;
    private Integer sort;

    public ProductImageVO toVO() {
        ProductImageVO vo = new ProductImageVO();
        BeanUtils.copyProperties(this, vo);
        return vo;
    }
}
