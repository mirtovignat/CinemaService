
package com.example.demo.dto.movie.tmdb;

import lombok.Builder;

@Builder
public record TMDBSearchResultDTO(
        Long tmdbId,
        String title,
        String originalTitle,
        String releaseDate,
        String posterUrl
) {
}