package com.example.demo.dto.movie.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBCredits(
        @JsonProperty("cast") List<TMDBCast> tmdbCasts,
        @JsonProperty("crew") List<TMDBCrew> crew
) {
}