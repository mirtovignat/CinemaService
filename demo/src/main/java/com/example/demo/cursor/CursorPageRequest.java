package com.example.demo.cursor;

import lombok.Builder;

@Builder
public record CursorPageRequest(
        String cursor,
        String sort
) {
    public SortDirection direction() {
        return SortDirection.from(sort);
    }
}