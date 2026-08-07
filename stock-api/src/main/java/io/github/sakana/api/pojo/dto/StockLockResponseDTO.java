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
public class StockLockResponseDTO {

    private Long lockId;
    private List<InsufficientSku> insufficientSkus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InsufficientSku {
        private Long skuId;
        private Integer required;
        private Integer available;
    }
}
