package io.github.sakana.product.pojo;

import io.github.sakana.product.enumeration.ProductSort;
import lombok.Data;

@Data
public class ProductPageQuery {

    private Integer offset;
    private Integer size;
    private Long categoryId;
    private ProductSort sort;
}
