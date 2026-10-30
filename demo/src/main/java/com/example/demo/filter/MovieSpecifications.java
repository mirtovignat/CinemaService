package com.example.demo.filter;

import com.example.demo.mapper.util.DurationMapper;
import com.example.demo.model.entity.Movie;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MovieSpecifications {

    private final DurationMapper durationMapper;
    private final MovieFilterValidator movieFilterValidator;

    public Specification<Movie> byFilter(MovieFilter movieFilter) {
        MovieFilter notEmptyMovieFilter = movieFilterValidator.orEmpty(movieFilter);

        return Specification.allOf(
                SpecificationUtils.greaterThanOrEqualToIfNotNull("price", notEmptyMovieFilter.priceFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("price", notEmptyMovieFilter.priceTo()),
                SpecificationUtils.greaterThanOrEqualToIfNotNull("releaseDate", notEmptyMovieFilter.releaseDateFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("releaseDate", notEmptyMovieFilter.releaseDateTo()),
                SpecificationUtils.greaterThanOrEqualToIfNotNull(
                        "duration",
                        durationMapper.toDuration(notEmptyMovieFilter.durationFrom())
                ),
                SpecificationUtils.lessThanOrEqualToIfNotNull(
                        "duration",
                        durationMapper.toDuration(notEmptyMovieFilter.durationTo())
                ),
                SpecificationUtils.greaterThanOrEqualToIfNotNull("rating", notEmptyMovieFilter.ratingFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("rating", notEmptyMovieFilter.ratingTo()),
                SpecificationUtils.arrayContains("genre", notEmptyMovieFilter.genres()),
                SpecificationUtils.inIfNotEmpty("director", notEmptyMovieFilter.directors()),
                SpecificationUtils.arrayContainsAny("actors", notEmptyMovieFilter.actors())
        );
    }

    public Specification<Movie> fieldStartsWith(String fieldName, String rawSearchQuery) {
        return movieFilterValidator.normalizeSearchQueryOptional(rawSearchQuery)
                .map(query -> SpecificationUtils.<Movie>fieldStartsWithNormalized(fieldName, query))
                .orElseGet(Specification::unrestricted);
    }

    public Specification<Movie> fieldStartsWithNormalized(String fieldName, String sanitizedSearchQuery) {
        return SpecificationUtils.fieldStartsWithNormalized(fieldName, sanitizedSearchQuery);
    }
}