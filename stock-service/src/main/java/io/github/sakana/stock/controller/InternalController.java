package io.github.sakana.stock.controller;

import io.github.sakana.api.pojo.dto.StockLockRequestDTO;
import io.github.sakana.api.pojo.dto.StockLockResponseDTO;
import io.github.sakana.common.result.Result;
import io.github.sakana.stock.pojo.entity.Lock;
import io.github.sakana.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/stock")
public class InternalController {

    @Autowired
    private StockService stockService;

    @PostMapping("/lock")
    public boolean lock(@RequestBody StockLockRequestDTO requestDTO) {
        boolean isSuccess = stockService.batchLock(requestDTO);
        return isSuccess;
    };
}
