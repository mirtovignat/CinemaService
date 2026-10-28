package com.example.demo.mapper.util;

import org.mapstruct.Named;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class RatingMapper {

    @Named("normalizeRating")
    public static BigDecimal normalizeRating(BigDecimal ratingValue) {
        if (ratingValue == null) {
            return null;
        }
        return ratingValue.setScale(1, RoundingMode.HALF_UP);
    }
}