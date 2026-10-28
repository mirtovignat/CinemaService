package com.example.demo.dto.movie.tmdb;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MoviePriceItem(
        @NotNull(message = "Не указан ID фильма")
        Long tmdbId,

        @NotNull(message = "Укажите цену для каждого фильма")
        @DecimalMin(value = "0.01", message = "Цена должна быть больше 0")
        BigDecimal price
) {
}