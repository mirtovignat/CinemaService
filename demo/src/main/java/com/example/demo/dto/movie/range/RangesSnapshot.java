package com.example.demo.dto.movie.range;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record RangesSnapshot(
        Range<BigDecimal> price,
        Range<Integer> duration,
        Range<LocalDate> releaseDate
) {
}