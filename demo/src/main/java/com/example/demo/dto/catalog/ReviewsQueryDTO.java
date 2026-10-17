package com.example.demo.dto.catalog;

import lombok.Builder;

@Builder
public record ReviewsQueryDTO(
        String cursor,
        String sort,
        Integer size
) {
    public enum SortDirection {
        ASC,
        DESC
    }
}