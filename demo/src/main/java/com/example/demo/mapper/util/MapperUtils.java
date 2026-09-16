package com.example.demo.mapper.util;

import com.example.demo.config.SecurityConfig;
import lombok.AllArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
@AllArgsConstructor
public class MapperUtils {

    private final SecurityConfig securityConfig;

    @Named("yearFromLocalDate")
    public Integer yearFromLocalDate(LocalDate localDate) {
        return localDate == null ? null : localDate.getYear();
    }

    @Named("rawToEncoded")
    public String encodePassword(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        return securityConfig.passwordEncoder().encode(rawPassword);
    }

    @Named("tofullName")
    public static String tofullName(String surname, String name, String patronymic) {
        if (surname == null || name == null) {
            return null;
        }
        if (patronymic != null && !patronymic.trim().isEmpty()) {
            return surname.trim() + " " + name.trim() + " " + patronymic.trim();
        }
        return surname.trim() + " " + name.trim();
    }

    @Named("extractSurname")
    public static String extractSurname(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 0 ? parts[0] : "";
    }

    @Named("extractName")
    public static String extractName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 1 ? parts[1] : "";
    }

    @Named("extractPatronymic")
    public static String extractPatronymic(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        return parts.length > 2 ? parts[2] : "";
    }

    @Named("calculateAge")
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}