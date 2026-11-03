package com.example.demo.mapper.util;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class RatingMapper {

    @Named("normalizeRating")
    public BigDecimal normalizeRating(BigDecimal ratingValue) {
        if (ratingValue == null) {
            return null;
        }
        return ratingValue.setScale(1, RoundingMode.HALF_UP);
    }
}