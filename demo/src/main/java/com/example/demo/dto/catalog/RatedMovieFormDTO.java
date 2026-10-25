package com.example.demo.dto.catalog;

import jakarta.validation.constraints.*;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RatedMovieFormDTO(
        @NotNull(message = "ID фильма не может быть пустым")
        Long id,

        @NotNull(message = "Оценка обязательна")
        @DecimalMin(value = "0.1", message = "Оценка не может быть меньше 0.1")
        @DecimalMax(value = "10.0", message = "Оценка не может быть больше 10")
        @Digits(integer = 2, fraction = 1, message = "Оценка должна быть от 0.0 до 10.0 с шагом 0.1")
        BigDecimal rating,

        @Size(max = 350, message = "Отзыв не может превышать 350 символов")
        String review
) {
}