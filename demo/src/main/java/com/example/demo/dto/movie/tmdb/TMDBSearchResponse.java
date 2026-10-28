package com.example.demo.dto.movie.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBSearchResponse(
        @JsonProperty("results") List<TMDBMovieDTO> results
) {
}