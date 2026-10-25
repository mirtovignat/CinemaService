package com.example.demo.dto.movie.range;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record MovieRangesSnapshot(
        Range<BigDecimal> price,
        Range<Long> duration,
        Range<LocalDate> releaseDate
) {
}