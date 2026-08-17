package io.github.sakana.order.enumeration;

import io.github.sakana.common.exception.BusinessException;

/**
 * 订单服务对外暴露的稳定业务错误码。
 */
public enum OrderErrorCode {

    PRODUCT_SERVICE_RESPONSE_INVALID(
            "ORDER_PRODUCT_SERVICE_RESPONSE_INVALID",
            "商品服务响应异常",
            502
    );

    private final String code;
    private final String message;
    private final int httpStatus;

    OrderErrorCode(String code, String message, int httpStatus) {
        this.code = code;
        this.message = message;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    public BusinessException exception() {
        return new BusinessException(code, message, httpStatus);
    }
}
