package io.github.sakana.api.client;

import io.github.sakana.api.pojo.dto.AddressDTO;
import io.github.sakana.common.result.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient("user-service")
public interface UserClient {

    @GetMapping("/internal/user/address/{id}")
    Result<AddressDTO> getAddress(@PathVariable("id") Long id);
}
