package com.example.demo.repository.projection;

import java.math.BigDecimal;

public interface MovieRatingStatsProjection {
    BigDecimal getAvgRating();
    Long getTotalCount();
}