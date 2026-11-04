package com.example.demo.cursor;

import lombok.Builder;

@Builder
public record CursorPageRequest(
        String cursor,
        String sort,
        String query
) {
    public CursorPageRequest {
        if (query != null) {
            String trimmed = query.trim();
            if (trimmed.isEmpty()) {
                query = null;
            } else {
                query = trimmed;
            }
        }
    }

    public SortDirection direction() {
        return SortDirection.from(sort);
    }
}