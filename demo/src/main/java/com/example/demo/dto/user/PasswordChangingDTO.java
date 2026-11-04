package com.example.demo.dto.user;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Builder;

import java.util.Objects;

@Builder
public record PasswordChangingDTO(
        @NotBlank(message = "Введите текущий пароль")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        String currentPassword,

        @NotBlank(message = "Введите пароль")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        String rawPassword,

        @NotBlank(message = "Повторите пароль")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        String repeatRawPassword
) {
    public void isMismatch() {
        if (!Objects.equals(rawPassword(), repeatRawPassword())) {
            throw BusinessException.of(ErrorCode.PASSWORDS_MISMATCH);
        }
    }
}