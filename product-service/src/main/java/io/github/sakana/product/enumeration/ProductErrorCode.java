package io.github.sakana.product.enumeration;

import io.github.sakana.common.exception.BusinessException;

/**
 * 商品服务对外暴露的稳定业务错误码。
 */
public enum ProductErrorCode {

    PRODUCT_ID_INVALID("PRODUCT_ID_INVALID", "商品ID不合法", 400),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "商品不存在", 404),
    PRODUCT_NOT_ON_SALE("PRODUCT_NOT_ON_SALE", "商品已下架", 409);

    private final String code;
    private final String message;
    private final int httpStatus;

    ProductErrorCode(String code, String message, int httpStatus) {
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

    public BusinessException exception(Object details) {
        return new BusinessException(code, message, httpStatus, details, null);
    }
}
