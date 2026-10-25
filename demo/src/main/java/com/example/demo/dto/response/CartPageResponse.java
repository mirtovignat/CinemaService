package com.example.demo.dto.response;

import com.example.demo.dto.catalog.CartSummaryDTO;
import lombok.Builder;

@Builder
public record CartPageResponse<DTO>(
        CursorPageResponse<DTO> cursorPageResponse,
        CartSummaryDTO cartSummaryDTO,
        Long catalogCount,
        String query
) {
}