package com.example.demo.controller;

import com.example.demo.web.interceptor.NavigationHistoryInterceptor;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BackController {

    @GetMapping("/back")
    public String back(HttpSession httpSession,
                       @RequestParam(value = "fallback", required = false, defaultValue = "/") String fallback) {

        Object storedAttribute = httpSession.getAttribute(NavigationHistoryInterceptor.PREVIOUS_PAGE_ATTR);
        httpSession.removeAttribute(NavigationHistoryInterceptor.PREVIOUS_PAGE_ATTR);

        String targetPath = (storedAttribute instanceof String storedPath && isSafeLocalPath(storedPath))
                ? storedPath
                : null;

        if (targetPath == null) {
            targetPath = isSafeLocalPath(fallback) ? fallback : "/";
        }
        return "redirect:" + targetPath;
    }

    private boolean isSafeLocalPath(String path) {
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