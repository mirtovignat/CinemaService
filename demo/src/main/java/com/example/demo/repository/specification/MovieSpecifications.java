package com.example.demo.repository.specification;

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

    private static final int IN_CHUNK_SIZE = 500;

    private static final String[] CHARS_TO_REMOVE = {
            " ", "-", ":", ",", ".", "!", "?", "—", "'", "\"", "«", "»"
    };

    public Specification<Movie> byFilter(MovieFilter movieFilter) {
        if (movieFilter == null) {
            return Specification.unrestricted();
        }

        return Stream.of(
                        movieFilter.priceFrom() != null ? greaterThanOrEqualTo("price", movieFilter.priceFrom()) : null,
                        movieFilter.priceTo() != null ? lessThanOrEqualTo("price", movieFilter.priceTo()) : null,
                        movieFilter.releaseDateFrom() != null ? greaterThanOrEqualTo("releaseDate", movieFilter.releaseDateFrom()) : null,
                        movieFilter.releaseDateTo() != null ? lessThanOrEqualTo("releaseDate", movieFilter.releaseDateTo()) : null,
                        movieFilter.durationFrom() != null ? greaterThanOrEqualTo("duration", Duration.ofSeconds(movieFilter.durationFrom())) : null,
                        movieFilter.durationTo() != null ? lessThanOrEqualTo("duration", Duration.ofSeconds(movieFilter.durationTo())) : null,
                        movieFilter.ratingFrom() != null ? greaterThanOrEqualTo("rating", movieFilter.ratingFrom()) : null,
                        movieFilter.ratingTo() != null ? lessThanOrEqualTo("rating", movieFilter.ratingTo()) : null,
                        (movieFilter.genres() != null && !movieFilter.genres().isEmpty())
                                ? isInCollection("genre", movieFilter.genres()) : null,
                        (movieFilter.directors() != null && !movieFilter.directors().isEmpty())
                                ? isInCollection("director", movieFilter.directors()) : null
                )
                .filter(Objects::nonNull)
                .reduce(Specification.unrestricted(), Specification::and);
    }

    public Specification<Movie> titleStartsWith(String rawQuery) {
        if (rawQuery == null || rawQuery.isBlank()) {
            return Specification.unrestricted();
        }
        String sanitized = sanitizeSearchString(rawQuery);
        if (sanitized.isEmpty()) {
            return Specification.unrestricted();
        }
        return titleStartsWithNormalized(sanitized);
    }

    private String sanitizeSearchString(String input) {
        if (input == null) return "";
        String string = input.toLowerCase().replace('ё', 'е');
        for (String c : CHARS_TO_REMOVE) {
            string = string.replace(c, "");
        }
        return string;
    }

    private Specification<Movie> titleStartsWithNormalized(String sanitizedSearchQuery) {
        return (root, query, criteriaBuilder) -> {
            Expression<String> expression = criteriaBuilder.lower(root.get("title"));
            for (String c : CHARS_TO_REMOVE) {
                expression = criteriaBuilder.function("replace", String.class, expression,
                        criteriaBuilder.literal(c), criteriaBuilder.literal(""));
            }
            expression = criteriaBuilder.function("replace", String.class, expression,
                    criteriaBuilder.literal("ё"), criteriaBuilder.literal("е"));
            return criteriaBuilder.like(expression, sanitizedSearchQuery + "%");
        };
    }

    private <Y extends Comparable<? super Y>> Specification<Movie> greaterThanOrEqualTo(String fieldName, Y value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), value);
    }

    private <Y extends Comparable<? super Y>> Specification<Movie> lessThanOrEqualTo(String fieldName, Y value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), value);
    }

    private <Y> Specification<Movie> isInCollection(String fieldName, Collection<Y> collection) {
        if (collection == null || collection.isEmpty()) {
            return null;
        }
        List<Y> values = new ArrayList<>(collection);
        if (values.size() <= IN_CHUNK_SIZE) {
            return (root, query, criteriaBuilder) -> root.get(fieldName).in(values);
        }
        return (root, query, criteriaBuilder) -> {
            List<Predicate> ors = new ArrayList<>();
            for (int i = 0; i < values.size(); i += IN_CHUNK_SIZE) {
                List<Y> chunk = values.subList(i, Math.min(i + IN_CHUNK_SIZE, values.size()));
                ors.add(root.get(fieldName).in(chunk));
            }
            return criteriaBuilder.or(ors.toArray(new Predicate[0]));
        };
    }
}