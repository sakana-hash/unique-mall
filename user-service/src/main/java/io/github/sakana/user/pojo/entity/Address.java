package io.github.sakana.user.pojo.entity;

import io.github.sakana.common.entity.UpdatableEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address extends UpdatableEntity {

    private Long userId;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private boolean isDefault;
}
