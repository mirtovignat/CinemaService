package com.example.demo.dto.catalog;

import com.example.demo.dto.base.UserOwnership;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ReviewViewDTO(
        @JsonUnwrapped UserOwnership ownership,
        String username,
        BigDecimal ratingValue,
        String reviewText
) {
}