package com.example.demo.config;

import com.example.demo.web.interceptor.BadgeUpdater;
import com.example.demo.web.interceptor.CacheControlInterceptor;
import com.example.demo.web.interceptor.SessionInterceptor;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@AllArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final SessionInterceptor sessionInterceptor;
    private final BadgeUpdater badgeUpdater;
    private final CacheControlInterceptor cacheControlInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry interceptorRegistry) {
        interceptorRegistry.addInterceptor(sessionInterceptor)
                .addPathPatterns(
                        "/cart/**",
                        "/orders/**",
                        "/favourites/**",
                        "/profile/**",
                        "/rated/**",
                        "/history/**",
                        "/library/**",
                        "/watched/**"
                )
                .excludePathPatterns(
                        "/rated/*/reviews",
                        "/rated/rating/*",
                        "/rated/count/*",
                        "/logout"
                );
        interceptorRegistry.addInterceptor(badgeUpdater)
                .addPathPatterns("/**")
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/webjars/**", "/error", "/login", "/register");
        interceptorRegistry.addInterceptor(cacheControlInterceptor)
                .addPathPatterns(
                        "/",
                        "/login",
                        "/register",
                        "/movies/**",
                        "/personal-account",
                        "/settings",
                        "/settings/change/field",
                        "/settings/delete/phone",
                        "/settings/change/pwd",
                        "/wallet/top-up",
                        "/cart/**",
                        "/favourites/**",
                        "/library/**",
                        "/history/**",
                        "/rated/**",
                        "/watched/**"
                )
                .excludePathPatterns("/css/**", "/js/**", "/images/**", "/favicon.ico");
    }
}