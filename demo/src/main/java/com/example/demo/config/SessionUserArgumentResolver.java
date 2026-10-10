package com.example.demo.config;

import com.example.demo.dto.user.AppUserDetails;
import com.example.demo.dto.user.SessionUser;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
public class SessionUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.getParameterType().equals(SessionUser.class);
    }

    @Override
    public Object resolveArgument(@NotNull MethodParameter methodParameter,
                                  ModelAndViewContainer modelAndViewContainer,
                                  @NotNull NativeWebRequest nativeWebRequest,
                                  WebDataBinderFactory webDataBinderFactory) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserDetails appUserDetails) {
            return appUserDetails.getSessionUser();
        }
        return null;
    }
}