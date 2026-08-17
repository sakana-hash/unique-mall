package io.github.sakana.feign;

import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import feign.Response;
import io.github.sakana.common.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

class FeignResultErrorDecoderTest {

    private final FeignResultErrorDecoder decoder =
            new FeignResultErrorDecoder(new ObjectMapper());

    @Test
    @DisplayName("标准4xx响应转换为业务异常并保留详情")
    void shouldDecodeBusinessError() {
        Response response = response(404, """
                {
                  "code":"PRODUCT_SKU_NOT_FOUND",
                  "msg":"部分SKU不存在",
                  "data":{"skuIds":[1002]},
                  "success":false
                }
                """);

        Exception decoded = decoder.decode("ProductClient#getSkuTradeInfo", response);

        BusinessException exception = assertInstanceOf(BusinessException.class, decoded);
        assertEquals("PRODUCT_SKU_NOT_FOUND", exception.getCode());
        assertEquals("部分SKU不存在", exception.getMessage());
        assertEquals(404, exception.getHttpStatus());
        assertEquals(Map.of("skuIds", List.of(1002)), exception.getDetails());
        FeignException cause = assertInstanceOf(FeignException.class, exception.getCause());
        assertEquals(404, cause.status());
    }

    @Test
    @DisplayName("400和409标准响应同样转换为对应业务异常")
    void shouldPreserveHttpStatus() {
        BusinessException badRequest = assertInstanceOf(
                BusinessException.class,
                decoder.decode("client#method", response(400,
                        "{\"code\":\"PARAM_INVALID\",\"msg\":\"参数错误\"}"))
        );
        BusinessException conflict = assertInstanceOf(
                BusinessException.class,
                decoder.decode("client#method", response(409,
                        "{\"code\":\"STOCK_NOT_ENOUGH\",\"msg\":\"库存不足\"}"))
        );

        assertEquals(400, badRequest.getHttpStatus());
        assertEquals(409, conflict.getHttpStatus());
    }

    @Test
    @DisplayName("5xx响应继续交给Feign默认解码器")
    void shouldKeepServerErrorAsFeignException() {
        Exception decoded = decoder.decode("client#method", response(500,
                "{\"code\":\"INTERNAL_ERROR\",\"msg\":\"系统繁忙\"}"));

        FeignException exception = assertInstanceOf(FeignException.class, decoded);
        assertEquals(500, exception.status());
    }

    @Test
    @DisplayName("非标准JSON响应继续交给Feign默认解码器且保留响应体")
    void shouldKeepInvalidBodyAsFeignException() {
        Exception decoded = decoder.decode(
                "client#method",
                response(400, "upstream returned invalid response")
        );

        FeignException exception = assertInstanceOf(FeignException.class, decoded);
        assertEquals(400, exception.status());
        assertEquals("upstream returned invalid response", exception.contentUTF8());
    }

    @Test
    @DisplayName("空响应体继续交给Feign默认解码器")
    void shouldKeepEmptyBodyAsFeignException() {
        Response response = Response.builder()
                .status(404)
                .reason("Not Found")
                .request(request())
                .headers(Map.of())
                .build();

        Exception decoded = decoder.decode("client#method", response);

        FeignException exception = assertInstanceOf(FeignException.class, decoded);
        assertEquals(404, exception.status());
    }

    @Test
    @DisplayName("错误状态下的SUCCESS响应不转换为业务异常")
    void shouldRejectSuccessCodeInErrorResponse() {
        Exception decoded = decoder.decode("client#method", response(400,
                "{\"code\":\"SUCCESS\",\"msg\":\"成功\"}"));

        assertInstanceOf(FeignException.class, decoded);
    }

    private static Response response(int status, String body) {
        return Response.builder()
                .status(status)
                .reason("error")
                .request(request())
                .headers(Map.of("Content-Type", List.of("application/json")))
                .body(body, StandardCharsets.UTF_8)
                .build();
    }

    private static Request request() {
        return Request.create(
                Request.HttpMethod.GET,
                "http://product-service/internal/product/sku/trade-info",
                Map.of(),
                null,
                StandardCharsets.UTF_8
        );
    }
}
