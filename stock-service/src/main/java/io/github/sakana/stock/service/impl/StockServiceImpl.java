package io.github.sakana.stock.service.impl;

import io.github.sakana.api.pojo.dto.StockLockRequestDTO;
import io.github.sakana.api.pojo.dto.StockLockResponseDTO;
import io.github.sakana.stock.enumeration.StockStatus;
import io.github.sakana.stock.mapper.LockMapper;
import io.github.sakana.stock.mapper.StockMapper;
import io.github.sakana.stock.pojo.entity.Lock;
import io.github.sakana.stock.pojo.entity.Stock;
import io.github.sakana.stock.service.StockService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private LockMapper lockMapper;
    private static final long EXPIRE_SECONDS = 15 * 60;

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

    @Override
    @Transactional
    public boolean batchLock(StockLockRequestDTO requestDTO) {
        Long orderId = requestDTO.getOrderId();
        if (orderId == null) {
            throw new RuntimeException("订单id不能为空");
        }
        List<StockLockRequestDTO.StockLockItem> items = requestDTO.getItems();
        if (items == null) {
            throw new RuntimeException("库存扣存项不能为空");
        }
        items = items.stream().filter(Objects::nonNull).peek(item -> {
            if (item.getSkuId() == null) {
                throw new RuntimeException("sku id不能为空");
            }
            Integer quantity = item.getQuantity();
            if (quantity == null) {
                throw new RuntimeException("数量不能为空");
            }
            if (quantity <= 0) {
                throw new RuntimeException("数量不能小于等于0");
            }
            if (quantity > 999) {
                throw new RuntimeException("数量不能超过999");
            }
        }).toList();
        if (items.isEmpty()) {
            throw new RuntimeException("库存扣存项不能为空");
        }
        if (items.size() > 50) {
            throw new RuntimeException(String.format(
                    "库存扣存项不能超过50, 当前数目: %d", items.size()
            ));
        }

        List<Long> ids = items.stream().map(StockLockRequestDTO.StockLockItem::getSkuId).toList();
        Set<Long> uniqueSkuIds = new HashSet<>();
        for (Long skuId : ids) {
            if (!uniqueSkuIds.add(skuId)) {
                throw new RuntimeException(String.format("sku id不能重复: %d", skuId));
            }
        }

        List<Stock> stocks = stockMapper.selectForUpdateBySkuIds(ids);
        if (stocks.size() != ids.size()) {
            throw new RuntimeException("部分 sku不存在");
        }

        Map<Long, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getSkuId, Function.identity()));
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expireTime = now.plusSeconds(EXPIRE_SECONDS);

        // 校验所有库存是否充足
        List<StockLockResponseDTO.InsufficientSku> insufficientSkus = new ArrayList<>();
        for (StockLockRequestDTO.StockLockItem item : items) {
            Stock stock = stockMap.get(item.getSkuId());
            if (stock.getAvailableStock() < item.getQuantity()) {
                insufficientSkus.add(StockLockResponseDTO.InsufficientSku.builder()
                        .skuId(item.getSkuId())
                        .required(item.getQuantity())
                        .available(stock.getAvailableStock())
                        .build());
            }
        }
        if (!insufficientSkus.isEmpty()) {
            throw new RuntimeException("部分商品库存不足");
        }

        for (StockLockRequestDTO.StockLockItem item : items) {
            int rows = stockMapper.lockStock(item.getSkuId(), item.getQuantity(), now);
            if (rows == 0) {
                throw new RuntimeException(String.format(
                        "sku %d 库存不足", item.getSkuId()
                ));
            }
        }

        List<Lock> locks = items.stream().map(item -> Lock.builder()
                .skuId(item.getSkuId())
                .orderId(orderId)
                .quantity(item.getQuantity())
                .expireTime(expireTime)
                .build()).toList();
        int rows = lockMapper.batchInsert(locks);
        if (locks.size() != rows) {
            throw new RuntimeException("已存在锁记录，加锁失败");
        }

        return true;
    }
}
