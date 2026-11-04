package com.example.demo.dto.authorize;

import lombok.Builder;

@Builder
public record LoginFormDTO(
        String username,
        String password
) {
    public static LoginFormDTO forSwitch(String username) {
        return LoginFormDTO.builder()
                .username(username)
                .password("")
                .build();
    }
}