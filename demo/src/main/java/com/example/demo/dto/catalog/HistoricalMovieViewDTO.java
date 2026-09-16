package com.example.demo.dto.catalog;

import com.example.demo.dto.base.MovieIdentity;
import com.example.demo.dto.base.MovieTitled;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricalMovieViewDTO(
        @JsonUnwrapped MovieIdentity id,
        @JsonUnwrapped MovieTitled title,
        BigDecimal priceSnapshot,
        LocalDateTime addedAt
) {
}