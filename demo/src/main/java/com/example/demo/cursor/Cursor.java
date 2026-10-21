package com.example.demo.cursor;

import lombok.Builder;

@Builder
public record Cursor(
        String value
) {
    public Cursor {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Cursor.value must not be blank");
        }
    }
}