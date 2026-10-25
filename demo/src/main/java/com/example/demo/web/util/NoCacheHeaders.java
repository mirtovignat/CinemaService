package com.example.demo.web.util;

import jakarta.servlet.http.HttpServletResponse;

public final class NoCacheHeaders {

    private static final String CACHE_CONTROL_VALUE = "no-store, no-cache, must-revalidate, max-age=0";

    public static void apply(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Cache-Control", CACHE_CONTROL_VALUE);
        httpServletResponse.setHeader("Pragma", "no-cache");
        httpServletResponse.setHeader("Expires", "0");
    }
}