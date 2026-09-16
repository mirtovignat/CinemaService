package com.example.demo.dto.catalog;

import com.example.demo.dto.base.*;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Builder
public record RatedMovieViewDTO(
        @JsonUnwrapped MovieIdentity id,
        @JsonUnwrapped MovieTitled title,
        @JsonUnwrapped MovieMedia media,
        @JsonUnwrapped MovieRating rating,
        @JsonUnwrapped MovieRelease release,
        @JsonUnwrapped UserOwnership ownership,
        BigDecimal ratingValue,
        String review,
        LocalDateTime addedAt
) {
}