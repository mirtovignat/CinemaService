package com.example.demo.filter;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MovieFilterValidator {

    public void validate(MovieFilter movieFilter) {
        validateRanges(movieFilter);
    }

    public void validateRanges(MovieFilter movieFilter) {
        validateRange(movieFilter.priceFrom(), movieFilter.priceTo());
        validateRange(movieFilter.ratingFrom(), movieFilter.ratingTo());
        validateRange(movieFilter.durationFrom(), movieFilter.durationTo());
        validateRange(movieFilter.releaseDateFrom(), movieFilter.releaseDateTo());
    }

    private <Value extends Comparable<Value>> void validateRange(Value from, Value to) {
        if (from != null && to != null && from.compareTo(to) > 0) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
    }

    public Optional<String> normalizeSearchQueryOptional(String rawSearchQuery) {
        if (rawSearchQuery == null || rawSearchQuery.isBlank()) {
            return Optional.empty();
        }
        String trimmed = rawSearchQuery.trim().toLowerCase().replace('ё', 'е');
        return trimmed.isBlank() ? Optional.empty() : Optional.of(trimmed);
    }

    public boolean hasAnySearchCriteria(MovieFilter movieFilter, String title) {
        return normalizeSearchQueryOptional(title).isPresent() || orEmpty(movieFilter).hasAnyCriteria();
    }

    public MovieFilter orEmpty(MovieFilter movieFilter) {
        return movieFilter != null ? movieFilter : MovieFilter.builder().build();
    }
}