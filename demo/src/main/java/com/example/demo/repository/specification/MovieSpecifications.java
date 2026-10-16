package com.example.demo.repository.specification;

import com.example.demo.filter.MovieFilter;
import com.example.demo.model.entity.Movie;
import com.example.demo.util.RangeValidator;
import com.example.demo.util.SpecificationUtils;
import com.example.demo.util.TitleNormalizer;
import jakarta.persistence.criteria.Expression;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

@Component
public class MovieSpecifications {

    public Specification<Movie> byFilter(MovieFilter movieFilter) {
        if (movieFilter == null) {
            return Specification.unrestricted();
        }
        validateFilterRanges(movieFilter);

        return Specification.allOf(
                SpecificationUtils.gteIfNotNull("price", movieFilter.priceFrom()),
                SpecificationUtils.lteIfNotNull("price", movieFilter.priceTo()),
                SpecificationUtils.gteIfNotNull("releaseDate", movieFilter.releaseDateFrom()),
                SpecificationUtils.lteIfNotNull("releaseDate", movieFilter.releaseDateTo()),
                SpecificationUtils.gteIfNotNull("duration", toDuration(movieFilter.durationFrom())),
                SpecificationUtils.lteIfNotNull("duration", toDuration(movieFilter.durationTo())),
                SpecificationUtils.gteIfNotNull("rating", movieFilter.ratingFrom()),
                SpecificationUtils.lteIfNotNull("rating", movieFilter.ratingTo()),
                SpecificationUtils.inIfNotEmpty("genre", movieFilter.genres()),
                SpecificationUtils.inIfNotEmpty("director", movieFilter.directors())
        );
    }

    public Specification<Movie> titleStartsWith(String rawSearchQuery) {
        if (rawSearchQuery == null || rawSearchQuery.isBlank()) {
            return Specification.unrestricted();
        }
        String sanitized = TitleNormalizer.normalize(rawSearchQuery);
        if (sanitized.isEmpty()) {
            return Specification.unrestricted();
        }
        return titleStartsWithNormalized(sanitized);
    }

    private void validateFilterRanges(MovieFilter movieFilter) {
        RangeValidator.validate(movieFilter.priceFrom(), movieFilter.priceTo(), BigDecimal::compareTo);
        RangeValidator.validate(movieFilter.ratingFrom(), movieFilter.ratingTo(), BigDecimal::compareTo);
        RangeValidator.validate(movieFilter.durationFrom(), movieFilter.durationTo(), Integer::compareTo);
        RangeValidator.validate(movieFilter.releaseDateFrom(), movieFilter.releaseDateTo(), LocalDate::compareTo);
    }

    private Specification<Movie> titleStartsWithNormalized(String sanitizedSearchQuery) {
        return (root, query, criteriaBuilder) -> {
            Expression<String> titleExpression = criteriaBuilder.lower(root.get("title"));
            for (String charToRemove : TitleNormalizer.CHARS_TO_REMOVE) {
                titleExpression = criteriaBuilder.function("replace", String.class, titleExpression,
                        criteriaBuilder.literal(charToRemove), criteriaBuilder.literal(""));
            }
            titleExpression = criteriaBuilder.function("replace", String.class, titleExpression,
                    criteriaBuilder.literal("ё"), criteriaBuilder.literal("е"));
            return criteriaBuilder.like(titleExpression, sanitizedSearchQuery + "%");
        };
    }

    private Duration toDuration(Integer seconds) {
        return seconds != null ? Duration.ofSeconds(seconds) : null;
    }
}