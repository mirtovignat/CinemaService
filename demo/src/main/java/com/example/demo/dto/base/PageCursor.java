package com.example.demo.dto.base;

import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record PageCursor(
        LocalDateTime addedAt,
        Long entityId
) {

    public String encode() {
        if (addedAt == null || entityId == null) {
            return null;
        }
        return addedAt + "_" + entityId;
    }

    public static PageCursor decode(String rawCursor) {
        if (rawCursor == null || rawCursor.isBlank()) {
            return null;
        }
        int separatorIndex = rawCursor.lastIndexOf('_');
        if (separatorIndex <= 0 || separatorIndex == rawCursor.length() - 1) {
            return null;
        }
        try {
            LocalDateTime addedAt = LocalDateTime.parse(rawCursor.substring(0, separatorIndex));
            Long entityId = Long.parseLong(rawCursor.substring(separatorIndex + 1));
            return PageCursor.builder()
                    .addedAt(addedAt)
                    .entityId(entityId)
                    .build();
        } catch (Exception exception) {
            return null;
        }
    }
}