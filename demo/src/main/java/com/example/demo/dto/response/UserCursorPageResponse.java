package com.example.demo.dto.response;

import com.example.demo.dto.user.UserSidebarViewDTO;
import lombok.Builder;

@Builder
public record UserCursorPageResponse<DataType>(
        CursorPageResponse<DataType> cursorPageResponse,
        UserSidebarViewDTO userSidebarViewDTO
) {
}