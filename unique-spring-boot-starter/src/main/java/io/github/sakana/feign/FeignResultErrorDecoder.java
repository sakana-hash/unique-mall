package io.github.sakana.feign;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.Util;
import feign.codec.ErrorDecoder;
import io.github.sakana.common.exception.BusinessException;
import io.github.sakana.common.result.Result;

import java.io.IOException;
import java.io.InputStream;

/**
 * 将下游服务的标准 4xx {@link Result} 错误响应恢复为业务异常。
 *
 * <p>5xx、空响应体和非标准响应仍由 Feign 默认解码器处理，防止把网络或系统故障
 * 误判为可预期的业务失败。</p>
 */
public class FeignResultErrorDecoder implements ErrorDecoder {

    private final ObjectMapper objectMapper;
    private final ErrorDecoder defaultDecoder = new ErrorDecoder.Default();

    public FeignResultErrorDecoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() < 400 || response.status() >= 500 || response.body() == null) {
            return defaultDecoder.decode(methodKey, response);
        }

        byte[] body;
        try (InputStream inputStream = response.body().asInputStream()) {
            body = Util.toByteArray(inputStream);
        } catch (IOException exception) {
            return defaultDecoder.decode(methodKey, response);
        }

        Response replayableResponse = response.toBuilder().body(body).build();
        try {
            Result<?> result = objectMapper.readerFor(Result.class)
                    .without(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(body);
            if (!isBusinessError(result)) {
                return defaultDecoder.decode(methodKey, replayableResponse);
            }

            Exception cause = defaultDecoder.decode(methodKey, replayableResponse);
            return new BusinessException(
                    result.getCode(),
                    result.getMsg(),
                    response.status(),
                    result.getData(),
                    cause
            );
        } catch (IOException | RuntimeException exception) {
            return defaultDecoder.decode(methodKey, replayableResponse);
        }
    }

    private static boolean isBusinessError(Result<?> result) {
        return result != null
                && result.getCode() != null
                && !result.getCode().isBlank()
                && !Result.SUCCESS_CODE.equals(result.getCode())
                && result.getMsg() != null
                && !result.getMsg().isBlank();
    }
}
