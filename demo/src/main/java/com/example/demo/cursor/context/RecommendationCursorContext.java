package com.example.demo.cursor.context;

import lombok.Builder;

import java.util.List;

@Builder
public record RecommendationCursorContext(
        Long userId,
        List<Long> excludedMovieIds
) {
}