package com.example.demo.dto.movie.range;

import lombok.Builder;

@Builder
public record MovieFilterMetaResponse(
        MovieRangesSnapshot movieRangesSnapshot
) {
}