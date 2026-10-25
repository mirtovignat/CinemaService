package com.example.demo.dto.response;

import lombok.Builder;

@Builder
public record WatchedMovieResponse(
        boolean saved,
        String message,
        Long historyItemId
) {
}