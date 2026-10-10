package com.example.demo.dto.catalog;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ReviewCursor(
        LocalDateTime addedAt,
        Long userId
) {
    public String encode() {
        return addedAt.toString() + "_" + userId;
    }

    public static ReviewCursor decode(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int index = raw.lastIndexOf('_');
        if (index <= 0 || index == raw.length() - 1) {
            return null;
        }
        try {
            return ReviewCursor.builder()
                    .addedAt(LocalDateTime.parse(raw.substring(0, index)))
                    .userId(Long.parseLong(raw.substring(index + 1)))
                    .build();
        } catch (Exception exception) {
            return null;
        }
    }
}