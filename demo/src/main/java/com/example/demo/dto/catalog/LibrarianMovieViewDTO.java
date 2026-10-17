package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record LibrarianMovieViewDTO(
        Long id,
        Long historyItemId,
        String title,
        String posterUrl,
        String genre,
        String director,
        BigDecimal rating,
        LocalDate releaseDate,
        Integer ageRating,
        Boolean watched,
        String addedAt
) {
}