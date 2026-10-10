package com.example.demo.dto.movie;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record MovieSearchDTO(
        Long id,
        String title,
        LocalDate releaseDate,
        String posterUrl,
        String genre,
        String director,
        String ageRating
) {}