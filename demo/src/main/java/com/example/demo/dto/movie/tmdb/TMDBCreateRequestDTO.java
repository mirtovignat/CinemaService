package com.example.demo.dto.movie.tmdb;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Builder;

import java.util.List;

@Builder
public record TMDBCreateRequestDTO(
        @NotEmpty(message = "Выберите хотя бы один фильм")
        @Valid
        List<MoviePriceItem> moviePriceItems
) {
}