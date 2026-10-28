package com.example.demo.dto.movie.tmdb;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public record TMDBMovieDTO(
        Long id,
        String title,
        @JsonProperty("original_title") String originalTitle,
        @JsonProperty("release_date") String releaseDate,
        @JsonProperty("poster_path") String posterPath,
        @JsonProperty("runtime") Integer runtime,
        @JsonProperty("overview") String overview,
        @JsonProperty("genres") List<TMDBGenre> genres,
        @JsonProperty("credits") TMDBCredits credits
) {
}