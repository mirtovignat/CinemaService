package com.example.demo.web;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.web.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

@Component
@AllArgsConstructor
public class SessionUserAdvice implements HandlerInterceptor {

    private final CurrentUserProvider currentUserProvider;

    @Override
    public void postHandle(@NotNull HttpServletRequest httpServletRequest,
                           @NotNull HttpServletResponse httpServletResponse,
                           @NotNull Object handler,
                           ModelAndView modelAndView) {
        if (modelAndView == null) {
            return;
        }
        SessionUser sessionUser = currentUserProvider.getSessionUser();
        if (sessionUser == null) {
            sessionUser = SessionUser.ANONYMOUS;
        }
        modelAndView.addObject("sessionUser", sessionUser);

        String currentUri = httpServletRequest.getRequestURI();
        if (httpServletRequest.getQueryString() != null) {
            currentUri += "?" + httpServletRequest.getQueryString();
        }
        modelAndView.addObject("currentUri", currentUri);
    }
}