package com.example.demo.cursor.context;

import lombok.Builder;

import java.util.Collection;

@Builder
public record RecommendationCursorContext(
        Long userId,
        Collection<Long> excludedMovieIds
) {
}