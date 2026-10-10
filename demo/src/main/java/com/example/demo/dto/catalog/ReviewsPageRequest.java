package com.example.demo.dto.catalog;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ReviewsPageRequest {

    private final ReviewCursor cursor;
    private final int size;
    private final ReviewsQueryDTO.SortDirection sort;

    public ReviewsQueryDTO.SortDirection getSort() {
        return sort != null ? sort : ReviewsQueryDTO.SortDirection.DESC;
    }

    public int safeSize() {
        if (size <= 0) {
            return 10;
        }
        return Math.min(size, 50);
    }

    public Long safeViewerId(Long viewerUserId) {
        return viewerUserId != null ? viewerUserId : -1L;
    }
}