package com.example.demo.web.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.lang.NonNull;

public final class RequestUtils {

    public static boolean isAjaxRequest(@NonNull HttpServletRequest httpServletRequest) {
        String accept = httpServletRequest.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }
}