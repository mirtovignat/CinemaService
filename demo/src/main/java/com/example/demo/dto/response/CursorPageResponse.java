package com.example.demo.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record CursorPageResponse<DTO>(
        List<DTO> content,
        boolean hasMore,
        String nextCursor,
        String sort
) {
}