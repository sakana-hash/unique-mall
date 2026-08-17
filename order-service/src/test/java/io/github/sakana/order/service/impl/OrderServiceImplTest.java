package io.github.sakana.order.service.impl;

import io.github.sakana.api.client.ProductClient;
import io.github.sakana.api.client.StockClient;
import io.github.sakana.api.client.UserClient;
import io.github.sakana.api.pojo.dto.SkuTradeDTO;
import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.common.result.Result;
import io.github.sakana.order.pojo.dto.CreateOrderRequestDTO;
import io.github.sakana.order.service.OrderPersistenceService;
import io.github.sakana.order.service.StockReleaseCompensationService;
import io.github.sakana.snowflake.SnowflakeIdGenerator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock
    private ProductClient productClient;
    @Mock
    private StockClient stockClient;
    @Mock
    private UserClient userClient;
    @Mock
    private SnowflakeIdGenerator snowflakeIdGenerator;
    @Mock
    private OrderPersistenceService orderPersistenceService;
    @Mock
    private StockReleaseCompensationService stockReleaseCompensationService;

    @InjectMocks
    private OrderServiceImpl orderService;

    @Test
    @DisplayName("商品服务的业务异常原样向上抛出")
    void shouldPropagateProductBusinessException() {
        CreateOrderRequestDTO request = validRequest();
        BusinessException downstreamException = new BusinessException(
                "PRODUCT_SKU_NOT_FOUND",
                "部分SKU不存在",
                404
        );
        when(productClient.getSkuTradeInfo(List.of(1001L)))
                .thenThrow(downstreamException);

        BusinessException thrown = assertThrows(
                BusinessException.class,
                () -> orderService.createOrder(request, 2001L)
        );

        assertSame(downstreamException, thrown);
    }

    @Test
    @DisplayName("商品服务返回非成功的200响应时按协议异常处理")
    void shouldRejectUnsuccessfulProductResponse() {
        CreateOrderRequestDTO request = validRequest();
        when(productClient.getSkuTradeInfo(List.of(1001L)))
                .thenReturn(Result.error("PRODUCT_UNKNOWN_ERROR", "商品查询失败"));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.createOrder(request, 2001L)
        );

        assertEquals("ORDER_PRODUCT_SERVICE_RESPONSE_INVALID", exception.getCode());
        assertEquals("商品服务响应异常", exception.getMessage());
        assertEquals(502, exception.getHttpStatus());
    }

    @Test
    @DisplayName("商品服务成功响应缺少交易信息时按协议异常处理")
    void shouldRejectEmptyProductData() {
        CreateOrderRequestDTO request = validRequest();
        when(productClient.getSkuTradeInfo(List.of(1001L)))
                .thenReturn(Result.<List<SkuTradeDTO>>success(List.of()));

        BusinessException exception = assertThrows(
                BusinessException.class,
                () -> orderService.createOrder(request, 2001L)
        );

        assertEquals("ORDER_PRODUCT_SERVICE_RESPONSE_INVALID", exception.getCode());
        assertEquals(502, exception.getHttpStatus());
    }

    private static CreateOrderRequestDTO validRequest() {
        return CreateOrderRequestDTO.builder()
                .addressId(3001L)
                .items(List.of(CreateOrderRequestDTO.Item.builder()
                        .skuId(1001L)
                        .quantity(2)
                        .build()))
                .build();
    }
}
