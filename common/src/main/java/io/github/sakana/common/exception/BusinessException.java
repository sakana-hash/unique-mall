package io.github.sakana.common.exception;

/**
 * 可预期的业务异常。
 *
 * <p>{@code code} 是供调用方稳定识别的业务错误码，{@code message} 是面向用户的错误提示，
 * {@code httpStatus} 决定异常响应的 HTTP 状态，{@code details} 可携带库存不足项、字段校验结果
 * 等结构化上下文。异常处理器不应直接向外暴露 {@link #getCause()} 中的内部实现信息。</p>
 */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_CODE = "BUSINESS_ERROR";
    public static final int DEFAULT_HTTP_STATUS = 400;

    private final String code;
    private final int httpStatus;
    private final transient Object details;

    public BusinessException(String message) {
        this(DEFAULT_CODE, message, DEFAULT_HTTP_STATUS, null, null);
    }

    public BusinessException(String message, Throwable cause) {
        this(DEFAULT_CODE, message, DEFAULT_HTTP_STATUS, null, cause);
    }

    public BusinessException(String code, String message) {
        this(code, message, DEFAULT_HTTP_STATUS, null, null);
    }

    public BusinessException(String code, String message, Object details) {
        this(code, message, DEFAULT_HTTP_STATUS, details, null);
    }

    public BusinessException(String code, String message, Throwable cause) {
        this(code, message, DEFAULT_HTTP_STATUS, null, cause);
    }

    public BusinessException(String code, String message, Object details, Throwable cause) {
        this(code, message, DEFAULT_HTTP_STATUS, details, cause);
    }

    public BusinessException(String code, String message, int httpStatus) {
        this(code, message, httpStatus, null, null);
    }

    public BusinessException(
            String code,
            String message,
            int httpStatus,
            Object details,
            Throwable cause
    ) {
        super(requireText(message, "message"), cause);
        this.code = requireText(code, "code");
        this.httpStatus = requireErrorStatus(httpStatus);
        this.details = details;
    }

    public String getCode() {
        return code;
    }

    public Object getDetails() {
        return details;
    }

    public int getHttpStatus() {
        return httpStatus;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + "不能为空");
        }
        return value;
    }

    private static int requireErrorStatus(int httpStatus) {
        if (httpStatus < 400 || httpStatus > 599) {
            throw new IllegalArgumentException("httpStatus必须是400到599之间的错误状态码");
        }
        return httpStatus;
    }
}
