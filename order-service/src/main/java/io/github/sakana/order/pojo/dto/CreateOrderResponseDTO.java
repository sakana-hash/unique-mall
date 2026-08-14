package io.github.sakana.order.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderResponseDTO {

    private Long orderId;
    private String orderNo;
    private Long payAmount;
    private Integer orderStatus;
    private LocalDateTime expireTime;
}
