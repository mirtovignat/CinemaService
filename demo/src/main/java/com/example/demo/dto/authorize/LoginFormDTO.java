package com.example.demo.dto.authorize;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;

@Builder
public record LoginFormDTO(
        @NotNull Identifier identifier,
        @NotBlank(message = "Введите username, email или телефон")
        String identifierValue,
        @NotBlank(message = "Введите пароль")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        String rawPassword
) {
}