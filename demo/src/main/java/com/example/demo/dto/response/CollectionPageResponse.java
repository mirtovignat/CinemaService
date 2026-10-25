package com.example.demo.dto.response;

import lombok.Builder;

@Builder
public record CollectionPageResponse<DTO>(
        CursorPageResponse<DTO> cursorPageResponse,
        Long catalogCount,
        String query
) {
}