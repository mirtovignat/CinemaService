package com.example.demo.filter;

import lombok.Builder;

@Builder
public record MovieSearchContext(
        String title,
        String sortBy,
        String sortDirection,
        String genre
) {

    public static MovieSearchContext of(String title, String sortBy, String sortDirection) {
        return MovieSearchContext.builder()
                .title(title)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }

    public static MovieSearchContext ofGenre(String genre, String sortBy, String sortDirection) {
        return MovieSearchContext.builder()
                .genre(genre)
                .sortBy(sortBy)
                .sortDirection(sortDirection)
                .build();
    }
}