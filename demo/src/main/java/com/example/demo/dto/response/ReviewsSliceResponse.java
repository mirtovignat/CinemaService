package com.example.demo.dto.response;

import com.example.demo.dto.catalog.ReviewViewDTO;
import lombok.Builder;

import java.math.BigDecimal;
import java.util.List;

@Builder
public record ReviewsSliceResponse(
        List<ReviewViewDTO> reviews,
        ReviewViewDTO ownReview,
        boolean hasMore,
        String nextCursor,
        BigDecimal avgRating,
        long totalReviews,
        String movieTitle,
        Long movieId,
        String sort,
        int pageSize
) {
}