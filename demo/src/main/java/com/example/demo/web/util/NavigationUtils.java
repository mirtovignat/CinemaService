package com.example.demo.web.util;

import com.example.demo.util.url.UrlSafety;
import com.example.demo.web.interceptor.NavigationHistoryInterceptor;
import jakarta.servlet.http.HttpSession;

public final class NavigationUtils {

    public static String resolvePreviousPage(HttpSession httpSession, String fallbackPath) {
        if (httpSession != null) {
            Object storedAttribute = httpSession.getAttribute(NavigationHistoryInterceptor.PREVIOUS_PAGE_ATTR);
            httpSession.removeAttribute(NavigationHistoryInterceptor.PREVIOUS_PAGE_ATTR);
            if (storedAttribute instanceof String storedPath && UrlSafety.isSafeLocalPath(storedPath)) {
                return storedPath;
            }
        }
        return UrlSafety.resolveFallbackLocalPath(fallbackPath, "/");
    }

    public static boolean isSafeLocalPath(String path) {
        return UrlSafety.isSafeLocalPath(path);
    }
}