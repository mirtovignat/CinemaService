package com.example.demo.dto.catalog;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record CartSummaryDTO(
        long count,
        BigDecimal total
) {
    public CartSummaryDTO {
        if (total == null) {
            total = BigDecimal.ZERO;
        }
    }

    public static CartSummaryDTO empty() {
        return CartSummaryDTO.builder()
                .count(0L)
                .total(BigDecimal.ZERO)
                .build();
    }
}