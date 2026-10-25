package com.example.demo.dto.response;

import com.example.demo.dto.catalog.ReviewViewDTO;
import lombok.Builder;

import java.math.BigDecimal;

@Builder
public record ReviewsPageResponse(
        CursorPageResponse<ReviewViewDTO> cursorPageResponse,
        ReviewViewDTO reviewViewDTO,
        Long movieId,
        String movieTitle,
        BigDecimal avgRating,
        Long totalReviews,
        String currentUsername
) {
}