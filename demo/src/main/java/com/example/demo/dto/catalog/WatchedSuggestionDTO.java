package com.example.demo.dto.catalog;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record WatchedSuggestionDTO(
        Long movieId,
        String title,
        Long watchCount,
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
        LocalDateTime lastWatchedAt
) {
}