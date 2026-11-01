package com.example.demo.dto.response;

import lombok.Builder;

@Builder
public record CollectionPageResponse<DTO>(
        CursorPageResponse<DTO> cursorPageResponse,
        Long catalogCount,
        String query
) {
    public static <DTO> CollectionPageResponse<DTO> of(
            CursorPageResponse<DTO> cursorPageResponse,
            Long catalogCount,
            String query) {
        return CollectionPageResponse.<DTO>builder()
                .cursorPageResponse(cursorPageResponse)
                .catalogCount(catalogCount)
                .query(query)
                .build();
    }
}