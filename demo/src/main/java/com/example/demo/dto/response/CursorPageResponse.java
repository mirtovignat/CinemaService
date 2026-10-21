package com.example.demo.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CursorPageResponse<Item>(
        List<Item> items,
        boolean hasMore,
        String nextCursor,
        String sort
) {
}