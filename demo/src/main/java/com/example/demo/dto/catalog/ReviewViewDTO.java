package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ReviewViewDTO(
        Long movieId,
        String username,
        BigDecimal ratingValue,
        String reviewText,
        String addedAt,
        boolean own,
        boolean deleted
) {
}