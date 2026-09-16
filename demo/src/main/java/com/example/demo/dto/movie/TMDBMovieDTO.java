package com.example.demo.dto.movie;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TMDBMovieDTO(
        Long id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("poster_path") String posterPath
) {
}