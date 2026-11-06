package com.example.demo.web.util;

import com.example.demo.util.text.StringSanitizer;
import com.example.demo.util.url.UrlSafety;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public final class RefererUtils {

    public static String resolveFallbackLocalPath(HttpServletRequest httpServletRequest) {
        String refererHeader = httpServletRequest.getHeader("Referer");
        if (StringSanitizer.trimToNull(refererHeader).isEmpty()) {
            return "/";
        }
        String localPath = extractInternalPath(refererHeader, httpServletRequest);
        return localPath != null ? localPath : "/";
    }

    public static String extractInternalPath(String refererHeader, HttpServletRequest httpServletRequest) {
        if (StringSanitizer.trimToNull(refererHeader).isEmpty()) {
            return null;
        }
        try {
            UriComponents refererUri = UriComponentsBuilder.fromUriString(refererHeader).build();

            String refererHost = refererUri.getHost();
            if (refererHost == null || !refererHost.equalsIgnoreCase(httpServletRequest.getServerName())) {
                return null;
            }

            int refererPort = refererUri.getPort();
            int serverPort = httpServletRequest.getServerPort();
            if (refererPort != -1 && refererPort != serverPort) {
                return null;
            }

            String refererPath = refererUri.getPath();
            if (!UrlSafety.isSafeLocalPath(refererPath)) {
                return null;
            }

            String refererQuery = refererUri.getQuery();
            if (refererQuery == null || refererQuery.isBlank()) {
                return refererPath;
            }
            return refererPath + "?" + refererQuery;

        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }
}