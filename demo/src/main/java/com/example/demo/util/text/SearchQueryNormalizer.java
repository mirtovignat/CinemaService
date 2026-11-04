package com.example.demo.util.text;

import java.util.Locale;
import java.util.Optional;

public final class SearchQueryNormalizer {

    public static boolean isMeaningful(String rawValue) {
        return rawValue != null && !rawValue.isBlank();
    }

    public static Optional<String> normalizeOptional(String rawValue) {
        if (!isMeaningful(rawValue)) {
            return Optional.empty();
        }
        String trimmedValue = rawValue.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
        return trimmedValue.isBlank() ? Optional.empty() : Optional.of(trimmedValue);
    }

    public static String normalizeOrEmpty(String rawValue) {
        return normalizeOptional(rawValue).orElse("");
    }

    public static String normalizeAndEscape(String rawValue) {
        return normalizeOptional(rawValue)
                .map(LikeEscaper::escape)
                .orElse("");
    }
}