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
    public void afterCompletion(@NotNull HttpServletRequest httpServletRequest,
                                @NotNull HttpServletResponse httpServletResponse,
                                @NotNull Object handler,
                                Exception exception) {
        String uri = httpServletRequest.getRequestURI();

        if (uri.startsWith("/login") || uri.startsWith("/register")) {
            NoCacheHeaders.applyAndClearSiteData(httpServletResponse);
        } else {
            NoCacheHeaders.apply(httpServletResponse);
        }
    }
}