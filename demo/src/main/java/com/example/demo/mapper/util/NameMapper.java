package com.example.demo.mapper.util;

import org.mapstruct.Named;

public final class NameMapper {

    @Named("toFullName")
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
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 0) {
            return parts[0];
        }
        return "";
    }

    @Named("extractName")
    public static String extractName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 1) {
            return parts[1];
        }
        return "";
    }

    @Named("extractPatronymic")
    public static String extractPatronymic(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length > 2) {
            return parts[2];
        }
        return "";
    }
}