package io.github.sakana.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.github.sakana.common.constant.HeadersConstant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class FeignHeaderInterceptor implements RequestInterceptor {
    @Override
    public void apply(RequestTemplate template) {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

        if (attributes == null) {
            return;
        }

        HttpServletRequest request = attributes.getRequest();

        copyHeader(request, template, HeadersConstant.USER_ID);
    }

    private void copyHeader(
            HttpServletRequest request,
            RequestTemplate template,
            String name
    ) {
        String value = request.getHeader(name);
        if (value != null && !value.isBlank()) {
            template.header(name, value);
        }
    }
}
