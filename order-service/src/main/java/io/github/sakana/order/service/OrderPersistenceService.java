package io.github.sakana.order.service;

import io.github.sakana.order.mapper.AddressMapper;
import io.github.sakana.order.mapper.ItemMapper;
import io.github.sakana.order.mapper.OrderMapper;
import io.github.sakana.order.pojo.entity.Address;
import io.github.sakana.order.pojo.entity.Item;
import io.github.sakana.order.pojo.entity.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class OrderPersistenceService {

    @Autowired
    private OrderMapper orderMapper;
    @Autowired
    private ItemMapper itemMapper;
    @Autowired
    private AddressMapper addressMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void insert(Order order, List<Item> items, Address address) {
        if (orderMapper.insert(order) != 1) {
            throw new RuntimeException("订单写入失败, orderId: " + order.getId());
        }
        if (itemMapper.batchInsert(items) != items.size()) {
            throw new RuntimeException("订单明细写入不完整, orderId: " + order.getId());
        }
        if (addressMapper.insert(address) != 1) {
            throw new RuntimeException("订单地址写入失败, orderId: " + order.getId());
        }
    }
}
