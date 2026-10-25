package com.example.demo.service.cursor;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.BiFunction;

@Service
public class SuggestionService {

    public <T> List<T> suggest(
            Long userId,
            String query,
            BiFunction<Long, String, List<T>> fetcher) {
        if (userId == null || query == null || query.isBlank()) {
            return List.of();
        }

        String normalized = query.trim().toLowerCase().replace('ё', 'е');

        if (normalized.isBlank()) {
            return List.of();
        }

        return fetcher.apply(userId, normalized);
    }
}