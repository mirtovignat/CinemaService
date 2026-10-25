package com.example.demo.web.resolver;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.web.security.CurrentUserProvider;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

@Component
@AllArgsConstructor
public class SessionUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentUserProvider currentUserProvider;

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        return SessionUser.class.equals(methodParameter.getParameterType());
    }

    @Override
    public Object resolveArgument(@NotNull MethodParameter methodParameter,
                                  ModelAndViewContainer modelAndViewContainer,
                                  @NotNull NativeWebRequest nativeWebRequest,
                                  WebDataBinderFactory webDataBinderFactory) {
        return currentUserProvider.getSessionUser();
    }
}