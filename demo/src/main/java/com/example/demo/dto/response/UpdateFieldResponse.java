package com.example.demo.dto.response;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import lombok.Builder;

@Builder
public record UpdateFieldResponse(
        UserForOwnerViewDTO userForOwnerViewDTO,
        SessionUser sessionUser
) {
    public static UpdateFieldResponse of(UserForOwnerViewDTO userForOwnerViewDTO,
                                         SessionUser sessionUser) {
        return UpdateFieldResponse.builder()
                .userForOwnerViewDTO(userForOwnerViewDTO)
                .sessionUser(sessionUser)
                .build();
    }
}