package com.example.demo.service.cursor;

import com.example.demo.util.text.SearchQueryNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class SuggestionService {

    private final int suggestionsLimit;

    public SuggestionService(@Value("${app.suggestions.dropdown-limit:10}") int suggestionsLimit) {
        this.suggestionsLimit = suggestionsLimit;
    }

    @FunctionalInterface
    public interface SuggestionFetcher<T> {
        List<T> fetch(Long userId, String normalizedQuery, int limit);
    }

    public <T> List<T> suggest(
            Long userId,
            String query,
            SuggestionFetcher<T> fetcher) {
        if (userId == null) {
            return List.of();
        }

        Optional<String> normalizedQuery = SearchQueryNormalizer.normalizeOptional(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        return fetcher.fetch(userId, normalizedQuery.get(), suggestionsLimit);
    }
}