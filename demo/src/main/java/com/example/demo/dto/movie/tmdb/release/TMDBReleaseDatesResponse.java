package com.example.demo.dto.movie.tmdb.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBReleaseDatesResponse(
        @JsonProperty("results") List<TMDBReleaseDateCountry> results
) {
}