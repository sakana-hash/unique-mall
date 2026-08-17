package io.github.sakana.product.pojo;

import io.github.sakana.product.enumeration.PageSort;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageQuery {

    private Integer offset;
    private Integer size;
    private Long categoryId;
    private PageSort sort;
}
