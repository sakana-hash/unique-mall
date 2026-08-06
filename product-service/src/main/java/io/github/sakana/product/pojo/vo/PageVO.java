package io.github.sakana.product.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {

    private List<T> items;
    private Long total;
    private Integer page;
    private Integer size;
    private Long pages;
}
