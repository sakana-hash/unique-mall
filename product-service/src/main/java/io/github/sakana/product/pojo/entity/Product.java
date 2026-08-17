package io.github.sakana.product.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import io.github.sakana.product.constant.ImageType;
import io.github.sakana.product.pojo.vo.ProductDetailVO;
import io.github.sakana.product.pojo.vo.ProductVO;
import lombok.Data;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

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

    public ProductVO toPageVO() {
        ProductVO vo = new ProductVO();
        vo.setProductId(getId());
        vo.setName(getName());
        vo.setMainImage(images.stream()
                .filter(image -> ImageType.MAIN_IMAGE.equals(image.getType()))
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

    public ProductDetailVO toDetailVO() {
        ProductDetailVO vo = new ProductDetailVO();
        vo.setImages(images.stream().map(ProductImage::toVO).collect(Collectors.toList()));
        vo.setSkus(skus.stream().map(ProductSKU::toVO).collect(Collectors.toList()));
        BeanUtils.copyProperties(this, vo);
        vo.setMinPrice(skus.stream()
                .map(ProductSKU::getPrice)
                .min(Long::compareTo)
                .orElse(null));
        return vo;
    }
}
