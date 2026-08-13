package io.github.sakana.api.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressDTO {

    private Long id;
    private String receiver;
    private String phone;
    private String province;
    private String city;
    private String district;
    private String detail;

}
