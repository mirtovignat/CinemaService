package com.example.demo.dto.movie;

import com.example.demo.dto.catalog.ReviewViewDTO;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Builder
public record MovieDetailsViewDTO(
        Long id,
        String title,
        String posterUrl,
        String genre,
        BigDecimal price,
        BigDecimal rating,
        LocalDate releaseDate,
        String director,
        String formattedDuration,
        String description,
        Long ratingsCount,
        Integer ageRating,
        String currencyCode,
        boolean bought,
        boolean inCart,
        boolean inFavourites,
        ReviewViewDTO ownRating,
        List<String> actors
) {
}