package com.example.demo.dto.authorize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record LoginFormDTO(
        @NotBlank(message = "Введите username, email или телефон")
        String username,

        @NotBlank(message = "Введите пароль")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        String password
) {
    public static LoginFormDTO forSwitch(String username) {
        return LoginFormDTO.builder()
                .username(username)
                .password("")
                .build();
    }
}