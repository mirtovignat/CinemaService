package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record CartItemViewDTO(
        Long id,
        String title,
        String posterUrl,
        String genre,
        BigDecimal price,
        BigDecimal rating,
        LocalDate releaseDate,
        String currencyCode,
        boolean inFavourites,
        boolean bought,
        short quantity,
        String formattedDuration
) {}