package io.github.sakana.product.pojo.dto;

import io.github.sakana.product.enumeration.PageSort;
import lombok.Data;

import static io.github.sakana.product.constant.ProductConstants.DEFAULT_PAGE_NUMBER;
import static io.github.sakana.product.constant.ProductConstants.DEFAULT_PAGE_SIZE;

@Data
public class ProductPageDTO {

    private Integer page = DEFAULT_PAGE_NUMBER;
    private Integer size = DEFAULT_PAGE_SIZE;
    private Long categoryId;
    private PageSort sort = PageSort.DEFAULT;
}
