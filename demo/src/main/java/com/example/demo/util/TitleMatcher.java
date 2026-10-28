package com.example.demo.util;

import java.util.Locale;

public final class TitleMatcher {

    private TitleMatcher() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase(Locale.ROOT).replace('ё', 'е');
    }

    public static boolean matchesExactly(String query, String title) {
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return false;
        }
        return normalizedQuery.equals(normalize(title));
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
}