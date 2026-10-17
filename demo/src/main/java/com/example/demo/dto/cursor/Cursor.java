package com.example.demo.dto.cursor;

import lombok.Builder;

@Builder
public record Cursor(
        String value
) {
}