package com.example.demo.dto.catalog;

import com.example.demo.cursor.SortDirection;
import lombok.Builder;

@Builder
public record ReviewsQueryDTO(
        String cursor,
        SortDirection sort,
        Integer size
) {
    public SortDirection direction() {
        return sort != null ? sort : SortDirection.DESC;
    }
}