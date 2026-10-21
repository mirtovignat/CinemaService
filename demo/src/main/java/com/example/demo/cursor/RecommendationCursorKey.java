package com.example.demo.cursor;

import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record RecommendationCursorKey(
        BigDecimal rating,
        Long identifier
) {
}