package com.example.demo.web.util;

import com.example.demo.web.interceptor.NavigationHistoryInterceptor;
import jakarta.servlet.http.HttpSession;

public final class NavigationUtils {

    public static String resolvePreviousPage(HttpSession httpSession, String fallbackPath) {
        if (httpSession != null) {
            Object storedAttribute = httpSession.getAttribute(NavigationHistoryInterceptor.PREVIOUS_PAGE_ATTR);
            httpSession.removeAttribute(NavigationHistoryInterceptor.PREVIOUS_PAGE_ATTR);
            if (storedAttribute instanceof String storedPath && isSafeLocalPath(storedPath)) {
                return storedPath;
            }
        }
        return isSafeLocalPath(fallbackPath) ? fallbackPath : "/";
    }

    public static boolean isSafeLocalPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (!path.startsWith("/")) {
            return false;
        }
        if (path.startsWith("//")) {
            return false;
        }
        return !path.startsWith("/\\");
    }
}