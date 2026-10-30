package com.example.demo.dto.movie.tmdb;

import lombok.Builder;

import java.util.ArrayList;
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

    public String describeCriteria() {
        List<String> parts = new ArrayList<>();
        if (tmdbIds != null && !tmdbIds.isEmpty()) {
            parts.add("ID TMDB");
        }
        if (titles != null && !titles.isEmpty()) {
            parts.add("название");
        }
        if (directors != null && !directors.isEmpty()) {
            parts.add("режиссёр");
        }
        if (actors != null && !actors.isEmpty()) {
            parts.add("актёр");
        }
        return parts.isEmpty() ? "не заданы" : String.join(", ", parts);
    }
}