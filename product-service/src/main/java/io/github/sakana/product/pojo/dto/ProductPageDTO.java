package io.github.sakana.product.pojo.dto;

import io.github.sakana.product.enumeration.PageSort;
import lombok.Data;

@Data
public class ProductPageDTO {

    private Integer page = 1;
    private Integer size = 20;
    private Long categoryId;
    private PageSort sort = PageSort.DEFAULT;
}
