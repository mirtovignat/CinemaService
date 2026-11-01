package com.example.demo.util.url;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class UrlSafety {

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

    public static String resolveFallbackLocalPath(String path, String fallbackPath) {
        return isSafeLocalPath(path) ? path : fallbackPath;
    }
}