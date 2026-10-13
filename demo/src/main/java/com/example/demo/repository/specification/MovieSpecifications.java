package com.example.demo.repository.specification;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.filter.MovieFilter;
import com.example.demo.model.entity.Movie;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

@Component
public class MovieSpecifications {

    private static final int IN_CLAUSE_CHUNK_SIZE = 500;

    private static final String[] CHARS_TO_REMOVE = {
            " ", "-", ":", ",", ".", "!", "?", "—", "'", "\"", "«", "»"
    };

    public Specification<Movie> byFilter(MovieFilter movieFilter) {
        if (movieFilter == null) {
            return Specification.unrestricted();
        }
        validateFilterRanges(movieFilter);

        return Stream.of(
                        movieFilter.priceFrom() != null
                                ? greaterThanOrEqualTo("price", movieFilter.priceFrom()) : null,
                        movieFilter.priceTo() != null
                                ? lessThanOrEqualTo("price", movieFilter.priceTo()) : null,
                        movieFilter.releaseDateFrom() != null
                                ? greaterThanOrEqualTo("releaseDate", movieFilter.releaseDateFrom()) : null,
                        movieFilter.releaseDateTo() != null
                                ? lessThanOrEqualTo("releaseDate", movieFilter.releaseDateTo()) : null,
                        movieFilter.durationFrom() != null
                                ? greaterThanOrEqualTo("duration", Duration.ofSeconds(movieFilter.durationFrom())) : null,
                        movieFilter.durationTo() != null
                                ? lessThanOrEqualTo("duration", Duration.ofSeconds(movieFilter.durationTo())) : null,
                        movieFilter.ratingFrom() != null
                                ? greaterThanOrEqualTo("rating", movieFilter.ratingFrom()) : null,
                        movieFilter.ratingTo() != null
                                ? lessThanOrEqualTo("rating", movieFilter.ratingTo()) : null,
                        (movieFilter.genres() != null && !movieFilter.genres().isEmpty())
                                ? isInCollection("genre", movieFilter.genres()) : null,
                        (movieFilter.directors() != null && !movieFilter.directors().isEmpty())
                                ? isInCollection("director", movieFilter.directors()) : null
                )
                .filter(Objects::nonNull)
                .reduce(Specification.unrestricted(), Specification::and);
    }

    private void validateFilterRanges(MovieFilter movieFilter) {
        if (movieFilter.priceFrom() != null && movieFilter.priceTo() != null
                && movieFilter.priceFrom().compareTo(movieFilter.priceTo()) > 0) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
        if (movieFilter.ratingFrom() != null && movieFilter.ratingTo() != null
                && movieFilter.ratingFrom().compareTo(movieFilter.ratingTo()) > 0) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
        if (movieFilter.durationFrom() != null && movieFilter.durationTo() != null
                && movieFilter.durationFrom() > movieFilter.durationTo()) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
        if (movieFilter.releaseDateFrom() != null && movieFilter.releaseDateTo() != null
                && movieFilter.releaseDateFrom().isAfter(movieFilter.releaseDateTo())) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
    }

    public Specification<Movie> titleStartsWith(String rawSearchQuery) {
        if (rawSearchQuery == null || rawSearchQuery.isBlank()) {
            return Specification.unrestricted();
        }
        String sanitizedSearchQuery = sanitizeSearchString(rawSearchQuery);
        if (sanitizedSearchQuery.isEmpty()) {
            return Specification.unrestricted();
        }
        return titleStartsWithNormalized(sanitizedSearchQuery);
    }

    private String sanitizeSearchString(String inputString) {
        if (inputString == null) return "";
        String sanitizedString = inputString.toLowerCase().replace('ё', 'е');
        for (String charToRemove : CHARS_TO_REMOVE) {
            sanitizedString = sanitizedString.replace(charToRemove, "");
        }
        return sanitizedString;
    }

    private Specification<Movie> titleStartsWithNormalized(String sanitizedSearchQuery) {
        return (root, query, criteriaBuilder) -> {
            Expression<String> titleExpression = criteriaBuilder.lower(root.get("title"));
            for (String charToRemove : CHARS_TO_REMOVE) {
                titleExpression = criteriaBuilder.function("replace", String.class, titleExpression,
                        criteriaBuilder.literal(charToRemove), criteriaBuilder.literal(""));
            }
            titleExpression = criteriaBuilder.function("replace", String.class, titleExpression,
                    criteriaBuilder.literal("ё"), criteriaBuilder.literal("е"));
            return criteriaBuilder.like(titleExpression, sanitizedSearchQuery + "%");
        };
    }

    private <Y extends Comparable<? super Y>> Specification<Movie> greaterThanOrEqualTo(
            String fieldName, Y value) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), value);
    }

    private <Y extends Comparable<? super Y>> Specification<Movie> lessThanOrEqualTo(
            String fieldName, Y value) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), value);
    }

    private <Y> Specification<Movie> isInCollection(String fieldName, Collection<Y> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        List<Y> values = new ArrayList<>(collection);
        if (values.size() <= IN_CLAUSE_CHUNK_SIZE) {
            return (root, query, criteriaBuilder) -> root.get(fieldName).in(values);
        }
        return (root, query, criteriaBuilder) -> {
            List<Predicate> orPredicates = new ArrayList<>();
            for (int index = 0; index < values.size(); index += IN_CLAUSE_CHUNK_SIZE) {
                List<Y> chunk = values.subList(index,
                        Math.min(index + IN_CLAUSE_CHUNK_SIZE, values.size()));
                orPredicates.add(root.get(fieldName).in(chunk));
            }
            return criteriaBuilder.or(orPredicates.toArray(new Predicate[0]));
        };
    }
}