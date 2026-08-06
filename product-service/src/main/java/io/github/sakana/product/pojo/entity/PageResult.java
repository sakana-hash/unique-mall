package io.github.sakana.product.pojo.entity;

import lombok.Data;

import java.util.List;

@Data
public class PageResult {

    private List<Long> ids;
    private Long total;
}
