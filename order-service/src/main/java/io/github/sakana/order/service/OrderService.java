package io.github.sakana.order.service;

import io.github.sakana.order.pojo.dto.CreateOrderRequestDTO;
import io.github.sakana.order.pojo.entity.Order;

public interface OrderService {

    Order createOrder(CreateOrderRequestDTO requestDTO, Long userId);
}
