package io.github.sakana.order.controller;

import io.github.sakana.common.constant.HeadersConstant;
import io.github.sakana.common.result.Result;
import io.github.sakana.order.pojo.dto.CreateOrderRequestDTO;
import io.github.sakana.order.pojo.dto.CreateOrderResponseDTO;
import io.github.sakana.order.pojo.entity.Order;
import io.github.sakana.order.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    @PostMapping
    public Result<CreateOrderResponseDTO> createOrder(@RequestBody CreateOrderRequestDTO createOrderRequestDTO,
                                                      @RequestHeader(HeadersConstant.USER_ID) Long userId) {
        Order order = orderService.createOrder(createOrderRequestDTO, userId);
        CreateOrderResponseDTO createOrderResponseDTO = CreateOrderResponseDTO.builder()
                .orderId(order.getId())
                .orderNo(order.getOrderNo())
                .payAmount(order.getPayAmount())
                .orderStatus(order.getStatus())
                .expireTime(order.getExpireTime())
                .build();

        return Result.success(createOrderResponseDTO);
    }

}
