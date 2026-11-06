package com.example.demo.mapper.util;

import com.example.demo.dto.authorize.RegisterFormDTO;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

@Component
public class NameMapper {

    @Named("toFullName")
    public String toFullName(String surname, String name, String patronymic) {
        if (surname == null || name == null) {
            return null;
        }
        if (patronymic != null && !patronymic.trim().isEmpty()) {
            return surname.trim() + " " + name.trim() + " " + patronymic.trim();
        }
        return surname.trim() + " " + name.trim();
    }

    @Named("fullNameFromRegisterForm")
    public String fullNameFromRegisterForm(RegisterFormDTO registerFormDTO) {
        if (registerFormDTO == null) {
            return null;
        }
        return toFullName(registerFormDTO.surname(), registerFormDTO.name(), registerFormDTO.patronymic());
    }

    @Named("extractSurname")
    public String extractSurname(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    @Named("extractName")
    public String extractName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? parts[1] : "";
    }

    @Named("extractPatronymic")
    public String extractPatronymic(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 2 ? parts[2] : "";
    }
}