package com.example.demo.web.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestUtils {

    public static boolean isAjaxRequest(HttpServletRequest httpServletRequest) {
        if (httpServletRequest == null) {
            return false;
        }

        String requestedWith = httpServletRequest.getHeader("X-Requested-With");
        String accept = httpServletRequest.getHeader("Accept");
        String contentType = httpServletRequest.getHeader("Content-Type");

        return "XMLHttpRequest".equalsIgnoreCase(requestedWith)
                || (accept != null && accept.contains("application/json"))
                || (contentType != null && contentType.contains("application/json"));
    }
}