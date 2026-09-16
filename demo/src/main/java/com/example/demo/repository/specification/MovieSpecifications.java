package com.example.demo.repository.specification;

import com.example.demo.filter.MovieFilter;
import com.example.demo.model.Genre;
import com.example.demo.model.entity.Movie;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class MovieSpecifications {

    public Specification<Movie> byFilter(MovieFilter movieFilter) {
        if (movieFilter == null) {
            return Specification.unrestricted();
        }
        String rawTitle = movieFilter.title();
        String cleanTitle = (rawTitle != null && !rawTitle.isBlank())
                ? sanitizeSearchString(rawTitle)
                : null;

        return Stream.of(
                        cleanTitle != null ? titleStartsWithNormalized(cleanTitle) : null,
                        movieFilter.priceFrom() != null ? greaterThanOrEqualTo("price", movieFilter.priceFrom()) : null,
                        movieFilter.priceTo() != null ? lessThanOrEqualTo("price", movieFilter.priceTo()) : null,
                        movieFilter.releaseDateFrom() != null ? greaterThanOrEqualTo("releaseDate", movieFilter.releaseDateFrom()) : null,
                        movieFilter.releaseDateTo() != null ? lessThanOrEqualTo("releaseDate", movieFilter.releaseDateTo()) : null,
                        movieFilter.durationFrom() != null ? greaterThanOrEqualTo("duration", Duration.ofMinutes(movieFilter.durationFrom())) : null,
                        movieFilter.durationTo() != null ? lessThanOrEqualTo("duration", Duration.ofMinutes(movieFilter.durationTo())) : null,
                        movieFilter.ratingFrom() != null ? greaterThanOrEqualTo("rating", movieFilter.ratingFrom()) : null,
                        movieFilter.ratingTo() != null ? lessThanOrEqualTo("rating", movieFilter.ratingTo()) : null,
                        (movieFilter.genres() != null && !movieFilter.genres().isEmpty()) ? isGenreInCollection(movieFilter.genres()) : null,
                        (movieFilter.directors() != null && !movieFilter.directors().isEmpty()) ? isInCollection("director", movieFilter.directors()) : null
                )
                .filter(Objects::nonNull)
                .reduce(Specification.unrestricted(), Specification::and);
    }

    private String sanitizeSearchString(String input) {
        if (input == null) return "";
        return input.toLowerCase().replaceAll("[^a-zа-я0-9]", "");
    }

    private Specification<Movie> titleStartsWithNormalized(String sanitizedSearchQuery) {
        return (root, query, criteriaBuilder) -> {
            Expression<String> expr = criteriaBuilder.lower(root.get("title"));
            String[] charsToRemove = new String[]{" ", "-", ":", ",", ".", "!", "?", "—", "'", "\"", "«", "»"};
            for (String c : charsToRemove) {
                expr = criteriaBuilder.function("replace", String.class, expr, criteriaBuilder.literal(c), criteriaBuilder.literal(""));
            }
            return criteriaBuilder.like(expr, sanitizedSearchQuery + "%");
        };
    }

    private <Y extends Comparable<? super Y>> Specification<Movie> greaterThanOrEqualTo(String fieldName, Y value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(root.get(fieldName), value);
    }

    private <Y extends Comparable<? super Y>> Specification<Movie> lessThanOrEqualTo(String fieldName, Y value) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(root.get(fieldName), value);
    }

    private <Y> Specification<Movie> isInCollection(String fieldName, Collection<Y> collection) {
        return (root, query, criteriaBuilder) -> root.get(fieldName).in(collection);
    }

    private Specification<Movie> isGenreInCollection(Collection<String> genreDisplayNames) {
        return (root, query, criteriaBuilder) -> {
            Collection<Genre> genres = genreDisplayNames.stream()
                    .map(com.example.demo.model.Genre::fromDisplayName)
                    .collect(Collectors.toList());
            return root.get("genre").in(genres);
        };
    }
}