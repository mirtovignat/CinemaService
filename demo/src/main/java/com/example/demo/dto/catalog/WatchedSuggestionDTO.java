package com.example.demo.dto.catalog;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public record WatchedSuggestionDTO(
        Long movieId,
        String title,
        Long watchCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime lastWatchedAt
) {
}