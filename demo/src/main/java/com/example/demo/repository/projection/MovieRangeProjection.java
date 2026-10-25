package com.example.demo.repository.projection;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

public interface MovieRangeProjection {
    BigDecimal getMinPrice();

    BigDecimal getMaxPrice();

    Duration getMinDuration();

    Duration getMaxDuration();

    LocalDate getMinReleaseDate();

    LocalDate getMaxReleaseDate();
}