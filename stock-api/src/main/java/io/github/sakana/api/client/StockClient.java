package io.github.sakana.api.client;

import io.github.sakana.api.pojo.dto.StockConfirmRequestDTO;
import io.github.sakana.api.pojo.dto.StockLockRequestDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "stock-service")
public interface StockClient {

    @PostMapping("/internal/stock/lock")
    boolean lock(@RequestBody StockLockRequestDTO requestDTO);

    @PostMapping("/internal/stock/confirm")
    boolean confirm(@RequestBody StockConfirmRequestDTO requestDTO);

}
