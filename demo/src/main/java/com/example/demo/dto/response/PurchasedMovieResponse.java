package com.example.demo.dto.response;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record PurchasedMovieResponse(
        BigDecimal newBalance
) {
    public static PurchasedMovieResponse of(BigDecimal newBalance) {
        return PurchasedMovieResponse.builder()
                .newBalance(newBalance)
                .build();
    }
}