package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record HistoricalMovieViewDTO(
        Long id,
        String title,
        BigDecimal priceSnapshot,
        String addedAt
) {
}