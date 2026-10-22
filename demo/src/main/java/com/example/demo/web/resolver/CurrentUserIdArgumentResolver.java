package com.example.demo.web.resolver;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.web.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.InsufficientAuthenticationException;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@RequiredArgsConstructor
public class CurrentUserIdArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentUserProvider currentUserProvider;

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return methodParameter.hasParameterAnnotation(CurrentUserId.class)
                && Long.class.equals(methodParameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NotNull MethodParameter methodParameter,
                                  ModelAndViewContainer modelAndViewContainer,
                                  @NotNull NativeWebRequest nativeWebRequest,
                                  WebDataBinderFactory webDataBinderFactory) {
        CurrentUserId currentUserIdAnnotation = methodParameter.getParameterAnnotation(CurrentUserId.class);
        boolean required = currentUserIdAnnotation == null || currentUserIdAnnotation.required();

        SessionUser sessionUser = currentUserProvider.getSessionUser();

        if (required && !sessionUser.isAuthenticated()) {
            throw new InsufficientAuthenticationException("Требуется аутентификация");
        }
        return sessionUser.idOrNull();
    }
}