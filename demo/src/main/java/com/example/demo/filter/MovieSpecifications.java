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
        MovieFilter validatedMovieFilter = movieFilterValidator.orEmpty(movieFilter);

        boolean hasDirectorsExclude = validatedMovieFilter.directorsExclude() != null
                && !validatedMovieFilter.directorsExclude().isEmpty();

        Specification<Movie> directorSpecification = hasDirectorsExclude
                ? SpecificationUtils.notNullAndNotIn("director", validatedMovieFilter.directorsExclude())
                : SpecificationUtils.inIfNotEmpty("director", validatedMovieFilter.directors());

        boolean hasGenresExclude = validatedMovieFilter.genresExclude() != null
                && !validatedMovieFilter.genresExclude().isEmpty();

        Specification<Movie> genreSpecification = hasGenresExclude
                ? SpecificationUtils.notNullAndNotIn("genre", validatedMovieFilter.genresExclude())
                : SpecificationUtils.inIfNotEmpty("genre", validatedMovieFilter.genres());

        return Specification.allOf(
                SpecificationUtils.greaterThanOrEqualToIfNotNull("price", validatedMovieFilter.priceFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("price", validatedMovieFilter.priceTo()),
                SpecificationUtils.greaterThanOrEqualToIfNotNull("releaseDate", validatedMovieFilter.releaseDateFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("releaseDate", validatedMovieFilter.releaseDateTo()),
                SpecificationUtils.greaterThanOrEqualToIfNotNull(
                        "duration",
                        durationMapper.toDuration(validatedMovieFilter.durationFrom())
                ),
                SpecificationUtils.lessThanOrEqualToIfNotNull(
                        "duration",
                        durationMapper.toDuration(validatedMovieFilter.durationTo())
                ),
                SpecificationUtils.greaterThanOrEqualToIfNotNull("rating", validatedMovieFilter.ratingFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("rating", validatedMovieFilter.ratingTo()),
                genreSpecification,
                directorSpecification,
                SpecificationUtils.arrayContainsAny("actors", validatedMovieFilter.actors())
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