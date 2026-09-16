package com.example.demo.web.util;

import jakarta.servlet.http.HttpServletResponse;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class NoCacheHeaders {

    private static final String CACHE_CONTROL_VALUE =
            "no-store, no-cache, must-revalidate, max-age=0";

    public static void apply(HttpServletResponse response) {
        response.setHeader("Cache-Control", CACHE_CONTROL_VALUE);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Expires", "0");
    }

    public static void applyAndClearSiteData(HttpServletResponse response) {
        apply(response);
        response.setHeader("Clear-Site-Data", "\"cache\"");
    }
}