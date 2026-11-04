package com.example.demo.service.cursor;

import com.example.demo.util.text.SearchQueryNormalizer;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;

@Service
public class SuggestionService {

    public <T> List<T> suggest(
            Long userId,
            String query,
            BiFunction<Long, String, List<T>> fetcher) {
        if (userId == null) {
            return List.of();
        }

        Optional<String> normalizedQuery = SearchQueryNormalizer.normalizeOptional(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        return fetcher.apply(userId, normalizedQuery.get());
    }
}