package io.github.sakana.api.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuTradeDTO {

    private Long skuId;
    private Long productId;
    private String skuCode;
    private String productName;
    private String imageUrl;
    private Long price;

    private Integer skuStatus;
    private Integer productStatus;
}
