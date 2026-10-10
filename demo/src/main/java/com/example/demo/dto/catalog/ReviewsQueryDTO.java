package com.example.demo.dto.catalog;

import lombok.Builder;

@Builder
public record ReviewsQueryDTO(
        String cursor,
        String sort
) {
    public ReviewCursor cursorOrNull() {
        return ReviewCursor.decode(cursor);
    }

    public enum SortDirection {
        ASC,
        DESC
    }
}