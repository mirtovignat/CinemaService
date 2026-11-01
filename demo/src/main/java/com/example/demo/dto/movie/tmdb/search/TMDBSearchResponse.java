package com.example.demo.dto.movie.tmdb.search;

import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBSearchResponse(
        @JsonProperty("results") List<TMDBMovieDTO> results
) {
}