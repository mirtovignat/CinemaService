package com.example.demo.web.interceptor;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.BadgeService;
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
public class SessionContextInterceptor implements HandlerInterceptor {

    private final BadgeService badgeService;
    private final CurrentUserProvider currentUserProvider;

    @Override
    public void postHandle(@NotNull HttpServletRequest request,
                           @NotNull HttpServletResponse response,
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

        if (!sessionUser.isAuthenticated()) {
            modelAndView.addObject("badges", BadgeCountsDTO.builder().build());
            return;
        }
        modelAndView.addObject("badges", badgeService.getBadgeCounts(sessionUser.id()));
    }
}