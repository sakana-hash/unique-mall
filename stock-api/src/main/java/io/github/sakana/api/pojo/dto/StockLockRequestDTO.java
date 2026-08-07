package io.github.sakana.api.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockLockRequestDTO {

    private Long orderId;
    private List<StockLockItem> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StockLockItem {
        private Long skuId;
        private Integer quantity;
    }
}
