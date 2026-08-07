package io.github.sakana.stock.service.impl;

import io.github.sakana.stock.enumeration.StockStatus;
import io.github.sakana.stock.mapper.StockMapper;
import io.github.sakana.stock.pojo.entity.Stock;
import io.github.sakana.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;

    @Override
    public Map<Long, Stock> getStockFromSkuIds(List<Long> skuIds) {
        if (skuIds == null) {
            return Map.of();
        }

        skuIds = skuIds.stream().filter(Objects::nonNull).toList();
        int size = skuIds.size();
        if (size == 0) {
            return Map.of();
        } else if (size > 50) {
            throw new RuntimeException(String.format(
                    "批量获取sku库存的最大数量不能超过50, 当前有效数量: %d", size
            ));
        }

        List<Stock> stocks = stockMapper.selectBySkuIds(skuIds);

        return stocks.stream().map(stock -> {
            if (stock == null || stock.getSkuId() == null) {
                return null;
            }

            StockStatus status = StockStatus.from(stock.getAvailableStock());
            stock.setStatus(status);

            return Map.entry(stock.getSkuId(), stock);
        }).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
