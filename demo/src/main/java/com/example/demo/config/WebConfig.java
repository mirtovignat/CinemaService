package com.example.demo.config;

import com.example.demo.web.interceptor.CacheControlInterceptor;
import com.example.demo.web.interceptor.NavigationHistoryInterceptor;
import com.example.demo.web.resolver.SessionUserArgumentResolver;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
@AllArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final CacheControlInterceptor cacheControlInterceptor;
    private final NavigationHistoryInterceptor navigationHistoryInterceptor;
    private final SessionUserArgumentResolver sessionUserArgumentResolver;

    @Override
    public void addInterceptors(InterceptorRegistry interceptorRegistry) {
        interceptorRegistry.addInterceptor(cacheControlInterceptor)
                .addPathPatterns(
                        "/", "/login",
                        "/register", "/movies/**",
                        "/personal-account", "/profile/edit",
                        "/settings/**",
                        "/wallet/**",
                        "/cart/**", "/favourites/**",
                        "/library/**", "/history/**",
                        "/rated/**", "/watched/**"
                )
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/favicon.ico",
                        "/cart/count", "/favourites/count",
                        "/rated/count/**", "/rated/rating/**",
                        "/rated/*/reviews/more",
                        "/movies/*/rating"
                );

        interceptorRegistry.addInterceptor(navigationHistoryInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/css/**", "/js/**", "/images/**", "/webjars/**",
                        "/favicon.ico", "/error", "/api/**", "/back"
                );
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> handlerMethodArgumentResolvers) {
        handlerMethodArgumentResolvers.add(sessionUserArgumentResolver);
    }
}