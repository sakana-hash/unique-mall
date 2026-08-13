package io.github.sakana.stock.service;

import io.github.sakana.api.pojo.dto.StockLockRequestDTO;
import io.github.sakana.stock.pojo.entity.Stock;

import java.util.List;
import java.util.Map;

public interface StockService {

    Map<Long, Stock> getStockFromSkuIds(List<Long> ids);
    boolean batchLock(StockLockRequestDTO requestDTO);
    boolean confirmOrder(Long orderId);
}
