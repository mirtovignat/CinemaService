package com.example.demo.filter;

import lombok.Builder;

@Builder
public record MovieSearchContext(
        String title,
        String sortBy,
        String sortDir,
        String genre
) {

    public static MovieSearchContext of(String title, String sortBy, String sortDir) {
        return MovieSearchContext.builder()
                .title(title)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
    }

    public static MovieSearchContext ofGenre(String genre, String sortBy, String sortDir) {
        return MovieSearchContext.builder()
                .genre(genre)
                .sortBy(sortBy)
                .sortDir(sortDir)
                .build();
    }
}