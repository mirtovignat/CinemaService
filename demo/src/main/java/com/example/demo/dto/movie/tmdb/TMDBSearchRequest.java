package com.example.demo.dto.movie.tmdb;

import lombok.Builder;

import java.util.List;

@Builder
public record TMDBSearchRequest(
        List<Long> tmdbIds,
        List<String> titles,
        List<String> directors,
        List<String> actors
) {
    public boolean hasAnyIds() {
        return tmdbIds != null && !tmdbIds.isEmpty();
    }

    public boolean hasAnyFields() {
        return (titles != null && !titles.isEmpty())
                || (directors != null && !directors.isEmpty())
                || (actors != null && !actors.isEmpty());
    }
}