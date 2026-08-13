package io.github.sakana.stock.service.impl;

import io.github.sakana.api.pojo.dto.StockLockRequestDTO;
import io.github.sakana.api.pojo.dto.StockLockResponseDTO;
import io.github.sakana.stock.constant.LockStatusConstant;
import io.github.sakana.stock.constant.StockChangeType;
import io.github.sakana.stock.enumeration.StockStatus;
import io.github.sakana.stock.mapper.LockMapper;
import io.github.sakana.stock.mapper.RecordMapper;
import io.github.sakana.stock.mapper.StockMapper;
import io.github.sakana.stock.pojo.entity.Lock;
import io.github.sakana.stock.pojo.entity.Record;
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
    @Autowired
    private RecordMapper recordMapper;

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
        // todo 实现接口幂等性

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
            checkQuantity(quantity);
        }).toList();
        if (items.isEmpty()) {
            throw new RuntimeException("库存扣存项不能为空");
        }
        if (items.size() > 50) {
            throw new RuntimeException(String.format(
                    "库存扣存项不能超过50, 当前数目: %d", items.size()
            ));
        }

        List<Long> skuIds = items.stream().map(StockLockRequestDTO.StockLockItem::getSkuId).toList();
        Set<Long> uniqueSkuIds = new HashSet<>();
        for (Long skuId : skuIds) {
            if (!uniqueSkuIds.add(skuId)) {
                throw new RuntimeException(String.format("sku id不能重复: %d", skuId));
            }
        }

        List<Stock> stocks = stockMapper.selectForUpdateBySkuIds(skuIds);
        if (stocks.size() != skuIds.size()) {
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

        // todo 使用线程池并发修改加快速度
        for (StockLockRequestDTO.StockLockItem item : items) {
            int rows = stockMapper.lockStockBySkuId(item.getSkuId(), item.getQuantity(), now);
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

        List<Record> records = items.stream().map(item -> Record.builder()
                        .skuId(item.getSkuId())
                        .orderId(orderId)
                        .changeType(StockChangeType.LOCK)
                        .changeAmount(item.getQuantity())
                        .availableBefore(stockMap.get(item.getSkuId()).getAvailableStock())
                        .availableAfter(stockMap.get(item.getSkuId()).getAvailableStock() - item.getQuantity())
                        .lockedBefore(stockMap.get(item.getSkuId()).getLockedStock())
                        .lockedAfter(stockMap.get(item.getSkuId()).getLockedStock() + item.getQuantity())
                        .build())
                .toList();
        rows = recordMapper.batchInsert(records);
        if (records.size() != rows) {
            throw new RuntimeException("已存在日志流水记录，记录失败");
        }

        return true;
    }

    @Override
    @Transactional
    public boolean confirmOrder(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("orderId不能为空");
        }

        List<Lock> locks = lockMapper.selectForUpdateByOrderId(orderId);

        LocalDateTime now = LocalDateTime.now();
        locks = checkLocks(locks, now);

        if (locks.isEmpty()) {
            throw new RuntimeException(String.format(
                    "该订单没有锁定的库存, 订单号: %d",  orderId
            ));
        }

        List<Long> skuIds = locks.stream().map(Lock::getSkuId).toList();
        List<Stock> stocks = stockMapper.selectForUpdateBySkuIds(skuIds);
        if (stocks.size() != skuIds.size()) {
            throw new RuntimeException("部分 sku不存在");
        }
        Map<Long, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getSkuId, Function.identity()));

        for (Lock lock : locks) {
            int rows = stockMapper.deductStockBySkuId(lock.getSkuId(), lock.getQuantity(), now);
            if (rows == 0) {
                throw new RuntimeException("库存扣减失败, SkuId: " + lock.getSkuId());
            }
        }

        int rows = lockMapper.updateStatusByOrderId(orderId, LockStatusConstant.CONFIRMED, now);
        if (rows != locks.size()) {
            throw new RuntimeException("部分锁状态更新失败");
        }

        List<Record> records = locks.stream().map(lock -> Record.builder()
                        .skuId(lock.getSkuId())
                        .orderId(orderId)
                        .changeType(StockChangeType.DEDUCTION)
                        .changeAmount(lock.getQuantity())
                        .availableBefore(stockMap.get(lock.getSkuId()).getAvailableStock())
                        .availableAfter(stockMap.get(lock.getSkuId()).getAvailableStock())
                        .lockedBefore(stockMap.get(lock.getSkuId()).getLockedStock())
                        .lockedAfter(stockMap.get(lock.getSkuId()).getLockedStock() - lock.getQuantity())
                        .build())
                .toList();

        rows = recordMapper.batchInsert(records);
        if (records.size() != rows) {
            throw new RuntimeException("已存在日志流水记录，记录失败");
        }

        return true;
    }

    @Override
    @Transactional
    public boolean release(Long orderId) {
        if (orderId == null) {
            throw new RuntimeException("orderId不能为空");
        }

        List<Lock> locks = lockMapper.selectForUpdateByOrderId(orderId);

        locks = checkLocks(locks);

        if (locks.isEmpty()) {
            throw new RuntimeException(String.format(
                    "该订单没有锁定的库存, 订单号: %d",  orderId
            ));
        }

        List<Long> skuIds = locks.stream().map(Lock::getSkuId).toList();
        List<Stock> stocks = stockMapper.selectForUpdateBySkuIds(skuIds);
        if (stocks.size() != skuIds.size()) {
            throw new RuntimeException("部分 sku不存在");
        }
        Map<Long, Stock> stockMap = stocks.stream().collect(Collectors.toMap(Stock::getSkuId, Function.identity()));

        LocalDateTime now = LocalDateTime.now();
        for (Lock lock : locks) {
            int rows = stockMapper.releaseStockBySkuId(lock.getSkuId(), lock.getQuantity(), now);
            if (rows == 0) {
                throw new RuntimeException("锁定库存释放失败, SkuId: " + lock.getSkuId());
            }
        }

        int rows = lockMapper.updateStatusByOrderId(orderId, LockStatusConstant.RELEASED, now);
        if (rows != locks.size()) {
            throw new RuntimeException("部分锁状态更新失败");
        }

        List<Record> records = locks.stream().map(lock -> Record.builder()
                        .skuId(lock.getSkuId())
                        .orderId(orderId)
                        .changeType(StockChangeType.RELEASE)
                        .changeAmount(lock.getQuantity())
                        .availableBefore(stockMap.get(lock.getSkuId()).getAvailableStock())
                        .availableAfter(stockMap.get(lock.getSkuId()).getAvailableStock() + lock.getQuantity())
                        .lockedBefore(stockMap.get(lock.getSkuId()).getLockedStock())
                        .lockedAfter(stockMap.get(lock.getSkuId()).getLockedStock() - lock.getQuantity())
                        .build())
                .toList();

        rows = recordMapper.batchInsert(records);
        if (records.size() != rows) {
            throw new RuntimeException("已存在日志流水记录，记录失败");
        }

        return true;
    }

    private static List<Lock> checkLocks(List<Lock> locks, LocalDateTime now) {
        if (locks == null || locks.isEmpty()) {
            return new ArrayList<>();
        }

        return locks.stream().filter(Objects::nonNull).peek(lock -> {
            // 校验锁定的数量
            checkQuantity(lock.getQuantity());

            // 校验锁状态
            if (!Objects.equals(lock.getStatus(), LockStatusConstant.LOCKER)) {
                throw new RuntimeException("锁已过期或已释放");
            }

            // 校验过期时间
            if (now.isAfter(lock.getExpireTime())) {
                throw new RuntimeException("锁已过期");
            }
        }).toList();
    }

    private static List<Lock> checkLocks(List<Lock> locks) {
        if (locks == null || locks.isEmpty()) {
            return new ArrayList<>();
        }

        return locks.stream().filter(Objects::nonNull).peek(lock -> {
            // 校验锁定的数量
            checkQuantity(lock.getQuantity());

            // 校验锁状态
            if (!Objects.equals(lock.getStatus(), LockStatusConstant.LOCKER)) {
                throw new RuntimeException("锁已过期或已释放");
            }

        }).toList();
    }

    private static Integer checkQuantity(Integer quantity) {
        if (quantity == null) {
            throw new RuntimeException("数量不能为空");
        }
        if (quantity <= 0) {
            throw new RuntimeException("数量不能小于等于0");
        }
        if (quantity > 999) {
            throw new RuntimeException("数量不能超过999");
        }
        return quantity;
    }
}
