package com.example.demo.util;

import java.util.Locale;

public final class TitleMatcher {

    private static final String PUNCTUATION_REGEX = "[:.,!?'\"«»()\\-–—]";

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace('ё', 'е');
    }

    public static String normalizeAndStripPunctuation(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return "";
        }
        return normalized
                .replaceAll(PUNCTUATION_REGEX, " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    public static boolean matchesExactly(String query, String title) {
        String normalizedQuery = normalizeAndStripPunctuation(query);
        if (normalizedQuery.isEmpty()) {
            return false;
        }
        return normalizedQuery.equals(normalizeAndStripPunctuation(title));
    }

    public static boolean matchesAnyExactly(String query, String... titles) {
        if (titles == null) {
            return false;
        }
        for (String title : titles) {
            if (matchesExactly(query, title)) {
                return true;
            }
        }
        return false;
    }

    public static boolean containsIgnoreCase(String query, String... titles) {
        if (titles == null) {
            return false;
        }
        String normalizedQuery = normalizeAndStripPunctuation(query);
        if (normalizedQuery.isEmpty()) {
            return false;
        }
        for (String title : titles) {
            if (title == null) {
                continue;
            }
            if (normalizeAndStripPunctuation(title).contains(normalizedQuery)) {
                return true;
            }
        }
        return false;
    }
}