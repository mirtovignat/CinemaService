package com.example.demo.dto.movie.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBCast(
        String name
) {
}