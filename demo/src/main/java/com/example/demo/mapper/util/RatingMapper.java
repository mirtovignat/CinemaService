package com.example.demo.mapper.util;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Mapper(componentModel = "spring")
public interface RatingMapper {

    @Named("normalizeRating")
    static BigDecimal normalizeRating(BigDecimal ratingValue) {
        if (ratingValue == null) {
            return null;
        }
        return ratingValue.setScale(1, RoundingMode.HALF_UP);
    }
}