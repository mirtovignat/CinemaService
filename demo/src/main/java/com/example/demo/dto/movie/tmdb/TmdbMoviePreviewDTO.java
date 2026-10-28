package com.example.demo.dto.movie.tmdb;

import lombok.Builder;

import java.util.List;

@Builder
public record TmdbMoviePreviewDTO(
        String title,
        String director,
        Integer ageRating,
        Long durationSeconds,
        String genre,
        String description,
        List<String> actors,
        String releaseDate
) {
}