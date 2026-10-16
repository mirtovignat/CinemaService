package com.example.demo.web.util;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.lang.NonNull;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class RequestUtils {

    public static boolean isAjaxRequest(@NonNull HttpServletRequest httpServletRequest) {
        String accept = httpServletRequest.getHeader("Accept");
        return accept != null && accept.contains("application/json");
    }
}