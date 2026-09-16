package com.example.demo.dto.base;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record MovieRelease(
        LocalDate releaseDate
) {
    public Integer getReleaseYear() {
        return releaseDate != null ? releaseDate.getYear() : null;
    }
}