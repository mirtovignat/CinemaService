package com.example.demo.mapper.util;

import lombok.AllArgsConstructor;
import org.mapstruct.Named;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Period;

@Component
@AllArgsConstructor
public class MapperUtils {

    private final PasswordEncoder passwordEncoder;

    @Named("rawToEncoded")
    public String encodePassword(String rawPassword) {
        if (rawPassword == null) {
            return null;
        }
        return passwordEncoder.encode(rawPassword);
    }

    @Named("normalizeRating")
    public static BigDecimal normalizeRating(BigDecimal ratingValue) {
        if (ratingValue == null) {
            return null;
        }
        return ratingValue.setScale(1, RoundingMode.HALF_UP);
    }

    public static String toFullName(String surname, String name, String patronymic) {
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
        String[] fullNameParts = fullName.trim().split("\\s+");
        return fullNameParts.length > 0 ? fullNameParts[0] : "";
    }

    @Named("extractName")
    public static String extractName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] fullNameParts = fullName.trim().split("\\s+");
        return fullNameParts.length > 1 ? fullNameParts[1] : "";
    }

    @Named("extractPatronymic")
    public static String extractPatronymic(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] fullNameParts = fullName.trim().split("\\s+");
        return fullNameParts.length > 2 ? fullNameParts[2] : "";
    }

    @Named("calculateAge")
    public static int calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return 0;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }
}