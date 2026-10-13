package com.example.demo.web.resolver;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.web.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.lang.reflect.ParameterizedType;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class SessionUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentUserProvider currentUserProvider;

    @Override
    public boolean supportsParameter(MethodParameter methodParameter) {
        Class<?> type = methodParameter.getParameterType();
        if (SessionUser.class.equals(type)) {
            return true;
        }
        if (Optional.class.equals(type)) {
            return SessionUser.class.equals(resolveOptionalGeneric(methodParameter));
        }
        return false;
    }

    @Override
    public Object resolveArgument(@NotNull MethodParameter methodParameter,
                                  ModelAndViewContainer modelAndViewContainer,
                                  @NotNull NativeWebRequest nativeWebRequest,
                                  WebDataBinderFactory webDataBinderFactory) {
        if (Optional.class.equals(methodParameter.getParameterType())) {
            return currentUserProvider.getOptional();
        }
        return currentUserProvider.get();
    }

    private Class<?> resolveOptionalGeneric(MethodParameter methodParameter) {
        if (!(methodParameter.getGenericParameterType() instanceof ParameterizedType parameterizedType)) {
            return null;
        }
        Object typeArgument = parameterizedType.getActualTypeArguments()[0];
        return typeArgument instanceof Class<?> clazz ? clazz : null;
    }
}