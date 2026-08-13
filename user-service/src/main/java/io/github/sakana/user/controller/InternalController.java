package io.github.sakana.user.controller;

import io.github.sakana.api.pojo.dto.AddressDTO;
import io.github.sakana.common.constant.HeadersConstant;
import io.github.sakana.common.result.Result;
import io.github.sakana.user.pojo.entity.Address;
import io.github.sakana.user.service.AddressService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/user")
public class InternalController {

    @Autowired
    private AddressService addressService;

    @GetMapping("/address/{id}")
    Result<AddressDTO> getAddress(@PathVariable("id") Long id,
                                  @RequestHeader(HeadersConstant.USER_ID) Long userId) {
        Address address = addressService.getByIdAndUserId(id, userId);

        AddressDTO addressDTO = new AddressDTO();
        BeanUtils.copyProperties(address, addressDTO);
        return Result.success(addressDTO);
    }
}
