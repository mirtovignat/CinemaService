package com.example.demo.service.cursor;

import com.example.demo.config.properties.SuggestionsProperties;
import com.example.demo.util.text.SearchQueryNormalizer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@AllArgsConstructor
public class SuggestionService {

    private final SuggestionsProperties suggestionsProperties;

    @FunctionalInterface
    public interface SuggestionFetcher<T> {
        List<T> fetch(Long userId, String normalizedQuery, int limit);
    }

    public <T> List<T> suggest(
            Long userId,
            String searchQuery,
            SuggestionFetcher<T> fetcher) {
        if (userId == null) {
            return List.of();
        }

        Optional<String> normalizedQuery = SearchQueryNormalizer.normalizeOptional(searchQuery);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        return fetcher.fetch(userId, normalizedQuery.get(), suggestionsProperties.dropdownLimit());
    }
}