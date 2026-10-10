package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record RatedMovieViewDTO(
        Long id,
        String title,
        String posterUrl,
        String genre,
        BigDecimal rating,
        LocalDate releaseDate,
        BigDecimal ratingValue,
        String review,
        LocalDateTime addedAt
) {
}