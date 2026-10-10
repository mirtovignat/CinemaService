package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record HistoricalMovieViewDTO(
        Long id,
        String title,
        BigDecimal priceSnapshot,
        LocalDateTime addedAt
) {
}