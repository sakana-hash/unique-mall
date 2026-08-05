package io.github.sakana.product.pojo.vo;

import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {

    private List<T> items;
    private Long total;
    private Integer page;
    private Integer size;
    private Long pages;
}
