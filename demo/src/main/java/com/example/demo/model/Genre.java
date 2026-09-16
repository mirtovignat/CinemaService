package com.example.demo.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum Genre {
    ACTION("Боевик"),
    ADVENTURE("Приключения"),
    COMEDY("Комедия"),
    DRAMA("Драма"),
    FANTASY("Фантастика"),
    FANTASY_EPIC("Фэнтези");

    private final String displayName;

    public static Genre fromDisplayName(String displayName) {
        for (Genre genre : values()) {
            if (genre.displayName.equals(displayName)) {
                return genre;
            }
        }
        throw new IllegalArgumentException("Неизвестный жанр: " + displayName);
    }
}