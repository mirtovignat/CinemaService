package com.example.demo.dto.movie.tmdb;

import lombok.Builder;

@Builder
public record RefreshAllResult(
        int total,
        int updated,
        int skipped,
        int failed
) {
}