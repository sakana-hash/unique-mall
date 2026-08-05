package io.github.sakana.product.pojo.vo;

import lombok.Data;

@Data
public class ProductPageVO {

    private Long productId;
    private String name;
    private String mainImage;
    private Long minPrice;
}
