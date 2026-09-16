package com.example.demo.web.interceptor;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.ErrorCode;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.RequestUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class SessionInterceptor implements HandlerInterceptor {

    private final MessageUtils messageUtils;

    private void sendUnauthorized(HttpServletRequest httpServletRequest,
                                  HttpServletResponse httpServletResponse) throws Exception {
        String message = messageUtils.getError(ErrorCode.NOT_AUTHORIZED);
        if (RequestUtils.isAjaxRequest(httpServletRequest)) {
            httpServletResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpServletResponse.setContentType("application/json;charset=UTF-8");
            httpServletResponse.getWriter().write("{\"code\":\"NOT_AUTHORIZED\",\"message\":\"" + message + "\"}");
        } else {
            httpServletResponse.sendRedirect("/login");
        }
    }

    @Override
    public boolean preHandle(HttpServletRequest httpServletRequest,
                             @NotNull HttpServletResponse httpServletResponse,
                             @NotNull Object handler) throws Exception {
        String uri = httpServletRequest.getRequestURI();
        String method = httpServletRequest.getMethod();
        if (method.equals("GET") && (uri.equals("/") ||
                uri.startsWith("/login") ||
                uri.startsWith("/register") ||
                uri.startsWith("/error") ||
                uri.startsWith("/rated/") ||
                uri.startsWith("/movies/") ||
                uri.startsWith("/watched"))) {
            return true;
        }
        if (method.equals("POST") && uri.startsWith("/rated/")) {
            HttpSession session = httpServletRequest.getSession(false);
            if (session == null || session.getAttribute("sessionUser") == null) {
                sendUnauthorized(httpServletRequest, httpServletResponse);
                return false;
            }
            return true;
        }
        if (method.equals("POST") && uri.startsWith("/watched")) {
            HttpSession session = httpServletRequest.getSession(false);
            if (session == null || session.getAttribute("sessionUser") == null) {
                sendUnauthorized(httpServletRequest, httpServletResponse);
                return false;
            }
            return true;
        }
        HttpSession session = httpServletRequest.getSession(false);
        SessionUser user = (session != null) ? (SessionUser) session.getAttribute("sessionUser") : null;
        if (user == null) {
            sendUnauthorized(httpServletRequest, httpServletResponse);
            return false;
        }
        return true;
    }
}