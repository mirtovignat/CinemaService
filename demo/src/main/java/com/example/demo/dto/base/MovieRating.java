package com.example.demo.dto.base;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record MovieRating(
        BigDecimal rating
) {
}