
package com.example.demo.dto.movie.tmdb;

import lombok.Builder;

@Builder
public record TmdbSearchResultDTO(
        Long tmdbId,
        String title,
        String originalTitle,
        String releaseDate,
        String posterUrl
) {
}