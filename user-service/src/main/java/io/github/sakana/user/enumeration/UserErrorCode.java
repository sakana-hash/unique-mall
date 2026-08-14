package io.github.sakana.user.enumeration;

import io.github.sakana.common.exception.BusinessException;

/**
 * 用户服务对外暴露的稳定业务错误码。
 */
public enum UserErrorCode {

    REGISTER_REQUEST_REQUIRED("USER_REGISTER_REQUEST_REQUIRED", "注册信息不能为空", 400),
    LOGIN_REQUEST_REQUIRED("USER_LOGIN_REQUEST_REQUIRED", "登录信息不能为空", 400),
    USERNAME_REQUIRED("USER_USERNAME_REQUIRED", "用户名不能为空", 400),
    PASSWORD_REQUIRED("USER_PASSWORD_REQUIRED", "密码不能为空", 400),
    PASSWORD_LENGTH_INVALID(
            "USER_PASSWORD_LENGTH_INVALID",
            "密码长度必须在8到64个字符之间",
            400
    ),
    PASSWORD_FORMAT_INVALID(
            "USER_PASSWORD_FORMAT_INVALID",
            "密码只能由数字、大小写字母和下划线组成",
            400
    ),
    CLIENT_IP_REQUIRED("USER_CLIENT_IP_REQUIRED", "无法获取客户端IP", 400),
    USERNAME_ALREADY_EXISTS("USER_USERNAME_ALREADY_EXISTS", "用户名已存在", 409),
    INVALID_CREDENTIALS("USER_INVALID_CREDENTIALS", "用户名或密码错误", 401),
    ADDRESS_ID_INVALID("USER_ADDRESS_ID_INVALID", "收货地址ID不合法", 400),
    USER_ID_INVALID("USER_ID_INVALID", "用户ID不合法", 400),
    ADDRESS_NOT_FOUND("USER_ADDRESS_NOT_FOUND", "收货地址不存在", 404);

    private final String code;
    private final String message;
    private final int httpStatus;

    UserErrorCode(String code, String message, int httpStatus) {
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

    public BusinessException exception(Object details, Throwable cause) {
        return new BusinessException(code, message, httpStatus, details, cause);
    }
}
