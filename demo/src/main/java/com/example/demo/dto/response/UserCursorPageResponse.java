package com.example.demo.dto.response;

import com.example.demo.dto.user.UserSidebarViewDTO;
import lombok.Builder;

@Builder
public record UserCursorPageResponse<DTO>(
        CursorPageResponse<DTO> page,
        UserSidebarViewDTO userSidebar
) {

    public static <DTO> UserCursorPageResponse<DTO> of(CursorPageResponse<DTO> cursorPageResponse,
                                                       UserSidebarViewDTO userSidebarViewDTO) {
        return UserCursorPageResponse.<DTO>builder()
                .page(cursorPageResponse)
                .userSidebar(userSidebarViewDTO)
                .build();
    }
}