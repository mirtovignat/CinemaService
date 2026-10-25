package com.example.demo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PurchasedMovieResponse(
        BigDecimal newBalance
) {
}