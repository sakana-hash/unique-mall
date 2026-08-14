package io.github.sakana.common.result;

import java.io.Serializable;

/**
 * 统一异常响应。
 *
 * @param code      兼容现有 {@link Result} 的结果码，固定为 1
 * @param errorCode 可供调用方稳定识别的业务错误码
 * @param msg       面向调用方的错误提示
 * @param details   可选的结构化错误详情
 */
public record ErrorResult(
        int code,
        String errorCode,
        String msg,
        Object details
) implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final int FAILURE_CODE = 1;

    public static ErrorResult error(String errorCode, String msg, Object details) {
        return new ErrorResult(FAILURE_CODE, errorCode, msg, details);
    }
}
