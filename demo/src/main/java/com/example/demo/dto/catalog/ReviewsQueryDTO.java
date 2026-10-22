package com.example.demo.dto.catalog;

import com.example.demo.cursor.SortDirection;
import lombok.Builder;

@Builder
public record ReviewsQueryDTO(
        String cursor,
        SortDirection sortDirection,
        Integer size
) {
    public SortDirection direction() {
        return sortDirection != null ? sortDirection : SortDirection.DESC;
    }
}