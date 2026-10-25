package com.example.demo.dto.response;

import com.example.demo.dto.user.UserSidebarViewDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record UserCatalogPageResponse<DTO>(
        CursorPageResponse<DTO> cursorPageResponse,
        UserSidebarViewDTO userSidebarViewDTO,
        List<String> genres,
        String query
) {
}