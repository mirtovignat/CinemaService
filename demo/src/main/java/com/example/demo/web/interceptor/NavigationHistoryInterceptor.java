package com.example.demo.web.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.net.URI;
import java.util.Set;

@Component
public class NavigationHistoryInterceptor implements HandlerInterceptor {

    public static final String PREVIOUS_PAGE_ATTR = "appPreviousPage";
    public static final String BACK_PATH = "/back";

    private static final Set<String> EXCLUDED_PREFIXES = Set.of(
            "/css/", "/js/", "/images/", "/webjars/", "/api/"
    );

    private static final Set<String> EXCLUDED_EXACT = Set.of(
            "/favicon.ico", "/error", BACK_PATH
    );

    @Override
    public boolean preHandle(@NotNull HttpServletRequest httpServletRequest,
                             @NotNull HttpServletResponse httpServletResponse,
                             @NotNull Object handler) {

        if (!"GET".equalsIgnoreCase(httpServletRequest.getMethod())) {
            return true;
        }

        if ("XMLHttpRequest".equalsIgnoreCase(httpServletRequest.getHeader("X-Requested-With"))) {
            return true;
        }

        String uri = httpServletRequest.getRequestURI();
        if (isExcluded(uri)) {
            return true;
        }

        HttpSession httpSession = httpServletRequest.getSession(false);
        if (httpSession == null) {
            return true;
        }

        String referer = httpServletRequest.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return true;
        }

        String refererPath = extractInternalPath(referer, httpServletRequest);
        if (refererPath == null) {
            return true;
        }

        String currentPath = buildPathWithQuery(uri, httpServletRequest.getQueryString());

        if (refererPath.equals(currentPath) || refererPath.equals(uri)) {
            return true;
        }

        if (refererPath.equals(BACK_PATH)
                || refererPath.startsWith(BACK_PATH + "?")) {
            return true;
        }

        httpSession.setAttribute(PREVIOUS_PAGE_ATTR, refererPath);
        return true;
    }

    private boolean isExcluded(String uri) {
        for (String prefix : EXCLUDED_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return EXCLUDED_EXACT.contains(uri);
    }

    private String buildPathWithQuery(String path, String query) {
        if (query == null || query.isBlank()) return path;
        return path + "?" + query;
    }

    private String extractInternalPath(String referer, HttpServletRequest httpServletRequest) {
        try {
            URI uri = URI.create(referer);
            String host = uri.getHost();
            if (host == null) return null;

            if (!host.equalsIgnoreCase(httpServletRequest.getServerName())) return null;

            int refererPort = uri.getPort();
            int serverPort = httpServletRequest.getServerPort();
            if (refererPort != -1 && refererPort != serverPort) return null;

            String path = uri.getRawPath();
            if (path == null || path.isBlank()) return null;

            return buildPathWithQuery(path, uri.getRawQuery());
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }
}