package com.example.demo.dto.movie.tmdb.person;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBPersonSearchResponse(
        @JsonProperty("results") List<TMDBPerson> results
) {
}