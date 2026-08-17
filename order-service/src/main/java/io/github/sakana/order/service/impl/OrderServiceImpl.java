package io.github.sakana.order.service.impl;

import io.github.sakana.api.client.ProductClient;
import io.github.sakana.api.client.StockClient;
import io.github.sakana.api.client.UserClient;
import io.github.sakana.api.pojo.dto.AddressDTO;
import io.github.sakana.api.pojo.dto.SkuTradeDTO;
import io.github.sakana.api.pojo.dto.StockLockRequestDTO;
import io.github.sakana.common.result.Result;
import io.github.sakana.order.constant.OrderStatus;
import io.github.sakana.order.enumeration.OrderErrorCode;
import io.github.sakana.order.pojo.dto.CreateOrderRequestDTO;
import io.github.sakana.order.pojo.entity.Address;
import io.github.sakana.order.pojo.entity.Item;
import io.github.sakana.order.pojo.entity.Order;
import io.github.sakana.order.service.OrderPersistenceService;
import io.github.sakana.order.service.OrderService;
import io.github.sakana.order.service.StockReleaseCompensationService;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    @Autowired
    private ProductClient productClient;
    @Autowired
    private StockClient stockClient;
    @Autowired
    private UserClient userClient;
    @Autowired
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Autowired
    private OrderPersistenceService orderPersistenceService;
    @Autowired
    private StockReleaseCompensationService stockReleaseCompensationService;

    private static final Integer EXPIRETIME_SECONDS = 60 * 15;

    @Override
    public Order createOrder(CreateOrderRequestDTO requestDTO, Long userId) {
        if (userId == null || userId <= 0) {
            throw new RuntimeException("userId cannot be null or less than zero");
        }

        Long addressId = requestDTO.getAddressId();
        if (addressId == null || addressId <= 0) {
            throw new RuntimeException("addressId cannot be null or less than zero");
        }

        List<CreateOrderRequestDTO.Item> items = requestDTO.getItems();
        if (items == null) {
            throw new RuntimeException("items cannot be null or empty");
        }

        items = items.stream().filter(Objects::nonNull).peek(item -> {
            if (item.getSkuId() == null || item.getSkuId() <= 0) {
                throw new RuntimeException("skuId cannot be null or less than zero");
            }
            Integer quantity = item.getQuantity();
            if (quantity == null || quantity <= 0) {
                throw new RuntimeException("quantity cannot be null or less than zero");
            }
        }).toList();

        if (items.isEmpty()) {
            throw new RuntimeException("items cannot be null or empty");
        }
        if (items.size() > 50) {
            throw new RuntimeException("items cannot be greater than 50");
        }
        Map<Long, CreateOrderRequestDTO.Item> itemMap = items.stream().collect(Collectors.toMap(CreateOrderRequestDTO.Item::getSkuId, Function.identity()));

        Long orderId = snowflakeIdGenerator.nextId();
        String remark = requestDTO.getRemark();

        List<Long> skuIds = items.stream().map(CreateOrderRequestDTO.Item::getSkuId).toList();
        Result<List<SkuTradeDTO>> productResult = productClient.getSkuTradeInfo(skuIds);
        if (productResult == null || !productResult.isSuccess()) {
            throw OrderErrorCode.PRODUCT_SERVICE_RESPONSE_INVALID.exception();
        }

        List<SkuTradeDTO> skuTradeDTOs = productResult.getData();
        if (skuTradeDTOs == null || skuTradeDTOs.isEmpty()) {
            throw OrderErrorCode.PRODUCT_SERVICE_RESPONSE_INVALID.exception();
        }

        List<Item> orderItems = skuTradeDTOs.stream().filter(Objects::nonNull).map(dto -> Item.builder()
                .orderId(orderId)
                .skuId(dto.getSkuId())
                .productId(dto.getProductId())
                .productName(dto.getProductName())
                .skuCode(dto.getSkuCode())
                .imageUrl(dto.getImageUrl())
                .price(dto.getPrice())
                .quantity(itemMap.get(dto.getSkuId()).getQuantity())
                .amount(dto.getPrice() * itemMap.get(dto.getSkuId()).getQuantity())
                .build()).toList();
        long payAmount = orderItems.stream()
                .map(Item::getAmount)
                .reduce(0L, Math::addExact);

        Result<AddressDTO> addressDTOResult = userClient.getAddress(addressId);
        if (!addressDTOResult.isSuccess()) {
            throw new RuntimeException("获取收货地址失败: " + addressDTOResult.getMsg());
        }
        AddressDTO addressDTO = addressDTOResult.getData();
        if (addressDTO == null) {
            throw new RuntimeException("addressDTO cannot be null or empty");
        }
        Address address = Address.builder()
                .orderId(orderId)
                .receiver(addressDTO.getReceiver())
                .phone(addressDTO.getPhone())
                .province(addressDTO.getProvince())
                .city(addressDTO.getCity())
                .district(addressDTO.getDistrict())
                .detail(addressDTO.getDetail())
                .build();

        StockLockRequestDTO request = StockLockRequestDTO.builder()
                .orderId(orderId)
                .items(items.stream().map(item -> StockLockRequestDTO.StockLockItem.builder()
                        .skuId(item.getSkuId())
                        .quantity(item.getQuantity())
                        .build()).toList())
                .build();

        Order order = Order.builder()
                .orderNo(orderId.toString() + LocalDateTime.now())
                .userId(userId)
                .totalAmount(payAmount)
                .payAmount(payAmount)
                .status(OrderStatus.PENDING)
                .remark(remark)
                .expireTime(LocalDateTime.now().plusSeconds(EXPIRETIME_SECONDS))
                .build();
        order.setId(orderId);

        boolean stockLockCallCompleted = false;
        boolean stockLocked = false;
        try {
            boolean lockSucceeded = stockClient.lock(request);
            stockLockCallCompleted = true;
            if (!lockSucceeded) {
                throw new RuntimeException("库存锁定失败, orderId: " + orderId);
            }
            stockLocked = true;

            orderPersistenceService.insert(order, orderItems, address);
            return order;
        } catch (RuntimeException e) {
            if (stockLocked || !stockLockCallCompleted) {
                stockReleaseCompensationService.release(orderId, e);
            }
            throw e;
        }
    }
}
