package io.github.sakana.product.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class ProductDetailVO {

    private Long id;
    private Long categoryId;
    private String name;
    private String subtitle;
    private String brand;
    private Long minPrice;

    private String content;
    private List<ProductImageVO> images;
    private List<ProductSKUVO> skus;
}
