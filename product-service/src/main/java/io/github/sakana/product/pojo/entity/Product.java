package io.github.sakana.product.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import io.github.sakana.product.pojo.vo.ProductPageVO;
import lombok.Data;

import java.util.List;

@Data
public class Product extends UpdatableEntity {

    private Long categoryId;
    private String name;
    private String subtitle;
    private String brand;
    private Integer status;

    private String content;

    private List<ProductImage> images;
    private List<ProductSKU> skus;

    public ProductPageVO toPageVO() {
        ProductPageVO vo = new ProductPageVO();
        vo.setProductId(getId());
        vo.setName(getName());
        vo.setMainImage(images.stream()
                .filter(image -> image.getType() == 1)
                .map(ProductImage::getUrl)
                .findFirst()
                .orElse(null)
        );
        vo.setMinPrice(skus.stream()
                .map(ProductSKU::getPrice)
                .min(Long::compareTo)
                .orElse(null));
        return vo;
    }
}
