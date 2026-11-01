package com.example.demo.util.text;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StringSanitizer {

    public static Optional<String> trimToNull(String rawValue) {
        if (rawValue == null) {
            return Optional.empty();
        }
        String trimmedValue = rawValue.trim();
        return trimmedValue.isEmpty() ? Optional.empty() : Optional.of(trimmedValue);
    }

    public static String trimOrEmpty(String rawValue) {
        return rawValue == null ? "" : rawValue.trim();
    }

    public static String trimOrFallback(String rawValue, String fallbackValue) {
        return trimToNull(rawValue).orElse(fallbackValue);
    }
}