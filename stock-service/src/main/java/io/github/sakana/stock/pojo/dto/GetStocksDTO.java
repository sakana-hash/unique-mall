package io.github.sakana.stock.pojo.dto;

import lombok.Data;

import java.util.List;

@Data
public class GetStocksDTO {

    private List<Long> skuIds;
}
