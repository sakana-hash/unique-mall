package io.github.sakana.common.result;

import lombok.Data;

import java.io.Serializable;

/**
 * 后端统一返回结果
 * @param <T>
 */
@Data
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final String SUCCESS_CODE = "SUCCESS";

    private String code; // 成功为 SUCCESS，失败为具体业务错误码
    private String msg; // 错误信息
    private T data; // 数据

    public static <T> Result<T> success() {
        Result<T> result = new Result<>();
        result.code = SUCCESS_CODE;
        result.msg = "成功";
        return result;
    }

    public static <T> Result<T> success(T object) {
        Result<T> result = new Result<>();
        result.data = object;
        result.code = SUCCESS_CODE;
        result.msg = "成功";
        return result;
    }

    public static <T> Result<T> error(String errorCode, String msg) {
        return error(errorCode, msg, null);
    }

    public static <T> Result<T> error(String errorCode, String msg, T details) {
        Result<T> result = new Result<>();
        if (errorCode == null || errorCode.isBlank()) {
            throw new IllegalArgumentException("errorCode不能为空");
        }
        result.code = errorCode;
        result.msg = msg;
        result.data = details;
        return result;
    }

    public boolean isSuccess() {
        return SUCCESS_CODE.equals(code);
    }

}
