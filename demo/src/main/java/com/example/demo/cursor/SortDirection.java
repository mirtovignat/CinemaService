package com.example.demo.cursor;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String raw) {
        if (raw == null || raw.isBlank()) {
            return DESC;
        }
        return "ASC".equalsIgnoreCase(raw) ? ASC : DESC;
    }

    public String lowercase() {
        return name().toLowerCase();
    }
}