package com.example.demo.util.url;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class UrlSafety {

    public static boolean isSafeLocalPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (!path.startsWith("/")) {
            return false;
        }
        String decoded;
        try {
            decoded = URLDecoder.decode(path, StandardCharsets.UTF_8);
        } catch (IllegalArgumentException illegalArgumentException) {
            return false;
        }
        if (!decoded.startsWith("/")) {
            return false;
        }
        if (decoded.startsWith("//")) {
            return false;
        }
        if (decoded.startsWith("/\\")) {
            return false;
        }
        if (decoded.indexOf('\\') != -1) {
            return false;
        }
        for (int index = 0; index < decoded.length(); index++) {
            char currentCharacter = decoded.charAt(index);
            if (currentCharacter < 0x20 || currentCharacter == 0x7f) {
                return false;
            }
        }
        return true;
    }

    public static String resolveFallbackLocalPath(String path, String fallbackPath) {
        return isSafeLocalPath(path) ? path : fallbackPath;
    }
}