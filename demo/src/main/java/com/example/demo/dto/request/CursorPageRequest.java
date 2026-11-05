package com.example.demo.dto.request;

import com.example.demo.cursor.SortDirection;
import lombok.Builder;

@Builder
public record CursorPageRequest(
        String cursor,
        String sortDirection,
        String searchQuery
) {
    public CursorPageRequest {
        if (searchQuery != null) {
            String trimmed = searchQuery.trim();
            if (trimmed.isEmpty()) {
                searchQuery = null;
            } else {
                searchQuery = trimmed;
            }
        }
    }

    public SortDirection direction() {
        return SortDirection.from(sortDirection);
    }
}