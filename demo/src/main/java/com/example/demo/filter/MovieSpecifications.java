package com.example.demo.filter;

import com.example.demo.mapper.util.DurationMapper;
import com.example.demo.model.entity.Movie;
import jakarta.persistence.criteria.Join;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.List;

@AllArgsConstructor
@Component
public class MovieSpecifications {

    private final DurationMapper durationMapper;
    private final MovieFilterValidator movieFilterValidator;

    public Specification<Movie> byFilter(MovieFilter movieFilter) {
        MovieFilter validated = movieFilterValidator.orEmpty(movieFilter);

        return Specification.allOf(
                SpecificationUtils.greaterThanOrEqualToIfNotNull("price", validated.priceFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("price", validated.priceTo()),
                SpecificationUtils.greaterThanOrEqualToIfNotNull("releaseDate", validated.releaseDateFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("releaseDate", validated.releaseDateTo()),
                SpecificationUtils.greaterThanOrEqualToIfNotNull("duration", durationMapper.secondsToDuration(validated.durationFrom())),
                SpecificationUtils.lessThanOrEqualToIfNotNull("duration", durationMapper.secondsToDuration(validated.durationTo())),
                SpecificationUtils.greaterThanOrEqualToIfNotNull("rating", validated.ratingFrom()),
                SpecificationUtils.lessThanOrEqualToIfNotNull("rating", validated.ratingTo()),
                buildCollectionSpecification("genre", validated.genres(), validated.genresExclude()),
                buildCollectionSpecification("director", validated.directors(), validated.directorsExclude()),
                actorsAny(validated.actors())
        );
    }

    private Specification<Movie> buildCollectionSpecification(
            String fieldName,
            Collection<String> includeValues,
            Collection<String> excludeValues
    ) {
        if (!CollectionUtils.isEmpty(excludeValues)) {
            return SpecificationUtils.notNullAndNotIn(fieldName, excludeValues);
        }
        return SpecificationUtils.inIfNotEmpty(fieldName, includeValues);
    }

    public Specification<Movie> fieldStartsWith(String fieldName, String rawSearchQuery) {
        return movieFilterValidator.normalizeSearchQueryOptional(rawSearchQuery)
                .map(query -> SpecificationUtils.<Movie>fieldStartsWithNormalized(fieldName, query))
                .orElseGet(Specification::unrestricted);
    }

    public Specification<Movie> fieldStartsWithNormalized(String fieldName, String sanitizedSearchQuery) {
        return SpecificationUtils.fieldStartsWithNormalized(fieldName, sanitizedSearchQuery);
    }

    public Specification<Movie> actorsAny(Collection<String> actorNames) {
        if (CollectionUtils.isEmpty(actorNames)) {
            return Specification.unrestricted();
        }

        List<String> sanitized = actorNames.stream()
                .filter(name -> name != null && !name.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (sanitized.isEmpty()) {
            return Specification.unrestricted();
        }

        return (root, query, criteriaBuilder) -> {
            if (query != null) {
                query.distinct(true);
            }
            Join<Movie, Object> actorsJoin = root.join("actors");
            return actorsJoin.get("fullName").in(sanitized);
        };
    }
}