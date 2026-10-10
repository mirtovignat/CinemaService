package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Builder
public record LibrarianMovieViewDTO(
        Long id,
        String title,
        String posterUrl,
        String genre,
        BigDecimal rating,
        LocalDate releaseDate,
        Integer ageRating,
        boolean watched,
        LocalDateTime addedAt,
        Long historyItemId,
        String director
) {}