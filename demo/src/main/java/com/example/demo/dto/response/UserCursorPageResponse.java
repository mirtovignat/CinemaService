package com.example.demo.dto.response;

import com.example.demo.dto.user.UserSidebarViewDTO;

public record UserCursorPageResponse<DataType>(
        CursorPageResponse<DataType> page,
        UserSidebarViewDTO userSidebar
) {

    public static <DataType> UserCursorPageResponse<DataType> of(
            CursorPageResponse<DataType> page,
            UserSidebarViewDTO userSidebar) {
        return new UserCursorPageResponse<>(page, userSidebar);
    }
}