package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record ReviewViewDTO(
        Long movieId,
        String username,
        BigDecimal ratingValue,
        String reviewText,
        LocalDateTime addedAt,
        boolean own
) {}