package com.example.demo.dto.movie.range;

import lombok.Builder;

import java.util.List;

@Builder
public record MovieFilterMetaResponse(
        List<String> genres,
        List<String> directors,
        List<String> actors,
        MovieRangesSnapshot movieRangesSnapshot
) {
}