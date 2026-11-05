package com.example.demo.cursor;

public enum SortDirection {
    ASC,
    DESC;

    public static SortDirection from(String cursor) {
        if (cursor == null || cursor.isBlank()) {
            return DESC;
        }
        if ("ASC".equalsIgnoreCase(cursor)) {
            return ASC;
        }
        return DESC;
    }

    public String lowercase() {
        return name().toLowerCase();
    }
}