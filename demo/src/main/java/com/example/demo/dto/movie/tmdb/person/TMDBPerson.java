package com.example.demo.dto.movie.tmdb.person;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBPerson(
        Long id,
        @JsonProperty("known_for_department") String knownForDepartment
) {
}