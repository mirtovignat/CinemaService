package com.example.demo.dto.base;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

@Builder
public record PageCursor(
        Instant addedAt,
        Long entityId
) {

    public String encode() {
        if (addedAt == null || entityId == null) return null;
        String raw = addedAt.toEpochMilli() + "|" + entityId;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes());
    }

    public static PageCursor decode(String rawCursor) {
        if (rawCursor == null || rawCursor.isBlank()) return null;
        try {
            String decoded = new String(Base64.getUrlDecoder().decode(rawCursor));
            int idx = decoded.indexOf('|');
            if (idx <= 0 || idx == decoded.length() - 1) return null;
            return PageCursor.builder()
                    .addedAt(Instant.ofEpochMilli(Long.parseLong(decoded.substring(0, idx))))
                    .entityId(Long.parseLong(decoded.substring(idx + 1)))
                    .build();
        } catch (Exception e) {
            return null;
        }
    }

    public LocalDateTime addedAtUtc() {
        return LocalDateTime.ofInstant(addedAt, ZoneOffset.UTC);
    }
}