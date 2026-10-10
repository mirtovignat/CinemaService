package com.example.demo.web.util;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;

@Component
public class ReturnUrlHelper {

    private static final String RETURN_URL_ATTR = "returnUrl";

    public void saveReturnUrl(HttpSession httpSession, String url) {
        if (url != null && !url.isBlank()) {
            httpSession.setAttribute(RETURN_URL_ATTR, url);
        }
    }

    public String getAndClearReturnUrl(HttpSession httpSession) {
        String url = (String) httpSession.getAttribute(RETURN_URL_ATTR);
        httpSession.removeAttribute(RETURN_URL_ATTR);
        return url;
    }

    public String getReturnUrlOrDefault(HttpSession httpSession, String defaultUrl) {
        String url = getAndClearReturnUrl(httpSession);
        return url != null ? url : defaultUrl;
    }

    public void saveReturnUrlIfNotExists(HttpSession httpSession,
                                         HttpServletRequest httpServletRequest,
                                         String... excludedPaths) {
        if (httpSession.getAttribute(RETURN_URL_ATTR) != null) {
            return;
        }
        String referer = httpServletRequest.getHeader("Referer");
        if (referer == null || referer.isBlank()) {
            return;
        }
        for (String excluded : excludedPaths) {
            if (referer.contains(excluded)) {
                return;
            }
        }
        saveReturnUrl(httpSession, referer);
    }
}