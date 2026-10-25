package com.example.demo.dto.catalog;

import com.example.demo.cursor.SortDirection;
import lombok.Builder;

@Builder
public record ReviewsQueryDTO(
        String cursor,
        String sortDirection,
        Integer size
) {
    public SortDirection direction() {
        return SortDirection.from(sortDirection);
    }
}