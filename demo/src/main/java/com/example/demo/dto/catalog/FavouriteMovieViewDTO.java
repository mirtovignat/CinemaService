package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record FavouriteMovieViewDTO(
        Long id,
        String title,
        String posterUrl,
        String genre,
        BigDecimal price,
        BigDecimal rating,
        LocalDate releaseDate,
        String currencyCode,
        boolean inCart,
        boolean bought,
        String formattedDuration
) {}