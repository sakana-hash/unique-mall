package io.github.sakana.stock.controller;

import io.github.sakana.common.result.Result;
import io.github.sakana.stock.pojo.dto.GetStocksDTO;
import io.github.sakana.stock.pojo.entity.Stock;
import io.github.sakana.stock.pojo.vo.StockVO;
import io.github.sakana.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/stock")
public class StockController {

    @Autowired
    private StockService stockService;

    @PostMapping("/batch")
    public Result<Map<Long, StockVO>> getStocks(@RequestBody GetStocksDTO stocksDTO) {
        Map<Long, Stock> map = stockService.getStockFromSkuIds(stocksDTO.getSkuIds());
        Map<Long, StockVO> data = map.entrySet().stream().map(
                entity -> {
                    Stock stock = entity.getValue();
                    if (stock == null || stock.getStatus() == null) {
                        return null;
                    }

                    StockVO vo = new StockVO(stock.getStatus());
                    Long key = entity.getKey();
                    return Map.entry(key, vo);
                }
        ).filter(Objects::nonNull).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return Result.success(data);
    }
}
