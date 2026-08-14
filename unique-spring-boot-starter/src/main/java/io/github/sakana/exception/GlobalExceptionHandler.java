package io.github.sakana.exception;

import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.common.result.ErrorResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String REQUEST_INVALID = "REQUEST_INVALID";
    private static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    private static final String METHOD_NOT_ALLOWED = "METHOD_NOT_ALLOWED";
    private static final String MEDIA_TYPE_NOT_SUPPORTED = "MEDIA_TYPE_NOT_SUPPORTED";
    private static final String INTERNAL_ERROR = "INTERNAL_ERROR";

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResult> handleBusinessException(BusinessException exception) {
        log.warn("业务处理失败, code={}, message={}", exception.getCode(), exception.getMessage());
        return ResponseEntity.status(exception.getHttpStatus())
                .body(ErrorResult.error(
                        exception.getCode(), exception.getMessage(), exception.getDetails()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResult> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        return invalidRequest("请求参数校验失败", fieldErrors(exception));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<ErrorResult> handleBindException(BindException exception) {
        return invalidRequest("请求参数校验失败", fieldErrors(exception));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MissingRequestHeaderException.class,
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ErrorResult> handleInvalidRequest(Exception exception) {
        log.debug("请求参数格式错误: {}", exception.getMessage());
        return invalidRequest("请求参数格式错误", null);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResult> handleMethodNotAllowed(
            HttpRequestMethodNotSupportedException exception
    ) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ErrorResult.error(METHOD_NOT_ALLOWED, "请求方法不支持", null));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResult> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception
    ) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(ErrorResult.error(
                        MEDIA_TYPE_NOT_SUPPORTED, "请求内容类型不支持", null
                ));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ErrorResult> handleResourceNotFound(Exception exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ErrorResult.error(RESOURCE_NOT_FOUND, "请求资源不存在", null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResult> handleUnexpectedException(Exception exception) {
        log.error("未处理的系统异常", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResult.error(INTERNAL_ERROR, "系统繁忙，请稍后重试", null));
    }

    private static ResponseEntity<ErrorResult> invalidRequest(String message, Object details) {
        return ResponseEntity.badRequest()
                .body(ErrorResult.error(REQUEST_INVALID, message, details));
    }

    private static Map<String, String> fieldErrors(BindException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return errors;
    }
}
