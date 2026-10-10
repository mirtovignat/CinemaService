package com.example.demo.dto.movie;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record MovieCardViewDTO(
        Long id,
        String title,
        String posterUrl,
        String genre,
        BigDecimal price,
        BigDecimal rating,
        LocalDate releaseDate,
        String director,
        String formattedDuration,
        String description,
        Long ratingsCount,
        Integer ageRating,
        String currencyCode
) {}