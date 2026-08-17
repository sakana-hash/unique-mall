package io.github.sakana.product.enumeration;

import io.github.sakana.common.exception.BusinessException;

/**
 * 商品服务对外暴露的稳定业务错误码。
 */
public enum ProductErrorCode {

    PAGE_REQUEST_REQUIRED("PRODUCT_PAGE_REQUEST_REQUIRED", "分页查询参数不能为空", 400),
    CATEGORY_ID_INVALID("PRODUCT_CATEGORY_ID_INVALID", "商品分类ID不合法", 400),
    PRODUCT_ID_INVALID("PRODUCT_ID_INVALID", "商品ID不合法", 400),
    PRODUCT_NOT_FOUND("PRODUCT_NOT_FOUND", "商品不存在", 404),
    PRODUCT_NOT_ON_SALE("PRODUCT_NOT_ON_SALE", "商品已下架", 409),
    SKU_IDS_REQUIRED("PRODUCT_SKU_IDS_REQUIRED", "SKU ID列表不能为空", 400),
    SKU_QUERY_LIMIT_EXCEEDED(
            "PRODUCT_SKU_QUERY_LIMIT_EXCEEDED", "单次最多查询50个SKU", 400
    ),
    SKU_ID_INVALID("PRODUCT_SKU_ID_INVALID", "SKU ID不合法", 400),
    SKU_ID_DUPLICATED("PRODUCT_SKU_ID_DUPLICATED", "SKU ID不能重复", 400),
    SKU_NOT_FOUND("PRODUCT_SKU_NOT_FOUND", "部分SKU不存在", 404),
    SKU_NOT_AVAILABLE("PRODUCT_SKU_NOT_AVAILABLE", "部分SKU不可销售", 409);

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
