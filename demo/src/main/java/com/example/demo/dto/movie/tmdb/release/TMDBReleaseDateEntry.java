package com.example.demo.dto.movie.tmdb.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBReleaseDateEntry(String certification) {
}