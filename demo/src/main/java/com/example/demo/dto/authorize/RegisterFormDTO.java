package com.example.demo.dto.authorize;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.validation.annotation.ValidEmailDomain;
import com.example.demo.validation.annotation.ValidPhone;
import jakarta.validation.constraints.*;
import lombok.Builder;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

@Builder
public record RegisterFormDTO(
        @NotBlank(message = "Никнейм не может быть пустым")
        @Size(min = 6, max = 20, message = "Никнейм должен содержать от 6 до 20 символов")
        String username,

        @NotNull(message = "Заполните пол")
        boolean sex,

        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Past(message = "Дата рождения должна быть в прошлом")
        LocalDate birthDate,

        @NotBlank(message = "Email не может быть пустым")
        @Email(message = "Некорректный email адрес")
        @ValidEmailDomain(message = "Почтовый домен не существует")
        String email,

        @NotBlank(message = "Введите пароль")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        String rawPassword,

        @NotBlank(message = "Повторите пароль")
        @Size(min = 8, message = "Пароль минимум 8 символов")
        String repeatRawPassword,

        @ValidPhone
        String phone,

        @NotBlank(message = "Введите фамилию")
        @Size(min = 2, max = 30, message = "Фамилия должна быть от 2 до 30 символов")
        @Pattern(regexp = "^[А-ЯЁ][а-яё]+(?:-[А-ЯЁ][а-яё]+)?$")
        String surname,

        @NotBlank(message = "Введите имя")
        @Size(min = 2, max = 30, message = "Имя должно быть от 2 до 30 символов")
        @Pattern(regexp = "^[А-ЯЁ][а-яё]+(-[А-ЯЁ][а-яё]+)?$")
        String name,

        @Size(max = 30, message = "Отчество должно быть до 30 символов")
        @Pattern(regexp = "^(|[А-ЯЁ][а-яё]{3,29}(вич|вна|ич|на))$",
                message = "Неверный формат отчества")
        String patronymic,

        @NotNull
        @Pattern(regexp = "[A-Z]{3}", message = "Неверный формат валюты")
        String currencyCode
) {
    public void ifMismatch() {
        if (!Objects.equals(rawPassword(), repeatRawPassword())) {
            throw BusinessException.of(ErrorCode.PASSWORDS_MISMATCH);
        }
    }

    public static RegisterFormDTO getForm() {
        return RegisterFormDTO.builder()
                .currencyCode("RUB")
                .build();
    }

    public void validateAge() {
        if (birthDate != null) {
            int age = Period.between(birthDate, LocalDate.now()).getYears();
            if (age < 14) {
                throw BusinessException.of(ErrorCode.TOO_YOUNG);
            }
        }
    }
}