package com.example.demo.dto.movie.tmdb.release;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBReleaseDateCountry(
        @JsonProperty("iso_3166_1") String iso,
        @JsonProperty("release_dates") List<TMDBReleaseDateEntry> dates
) {
}