package com.example.demo.web.interceptor;

import com.example.demo.web.util.NoCacheHeaders;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class CacheControlInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(@NotNull HttpServletRequest httpServletRequest,
                             @NotNull HttpServletResponse httpServletResponse,
                             @NotNull Object handler) {
        String uri = httpServletRequest.getRequestURI();

        if (uri.startsWith("/js/")
                || uri.startsWith("/css/")
                || uri.startsWith("/images/")
                || uri.startsWith("/webjars/")
                || uri.equals("/favicon.ico")) {
            return true;
        }

        NoCacheHeaders.apply(httpServletResponse);
        return true;
    }
}