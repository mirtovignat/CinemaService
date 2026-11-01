package com.example.demo.dto.response;

import lombok.Builder;

@Builder
public record WatchedMovieResponse(
        boolean saved,
        String message,
        Long historyItemId
) {
    public static WatchedMovieResponse of(boolean saved, String message, Long historyItemId) {
        return WatchedMovieResponse.builder()
                .saved(saved)
                .message(message)
                .historyItemId(historyItemId)
                .build();
    }
}