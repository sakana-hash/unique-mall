package io.github.sakana.order.pojo.entity;

import io.github.sakana.common.entity.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends BaseEntity {

    private Long orderId;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
}
