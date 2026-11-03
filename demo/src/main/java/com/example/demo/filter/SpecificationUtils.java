package com.example.demo.filter;

import com.example.demo.util.text.LikeEscaper;
import com.example.demo.util.text.SearchQueryNormalizer;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SpecificationUtils {

    private static final int IN_CLAUSE_CHUNK_SIZE = 500;

    public static <Entity> Specification<Entity> unrestricted() {
        return Specification.unrestricted();
    }

    public static <Entity, Value extends Comparable<? super Value>> Specification<Entity> greaterThanOrEqualToIfNotNull(
            String fieldName,
            Value value
    ) {
        if (value == null) {
            return unrestricted();
        }
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.greaterThanOrEqualTo(
                root.get(fieldName),
                value
        );
    }

    public static <Entity, Value extends Comparable<? super Value>> Specification<Entity> lessThanOrEqualToIfNotNull(
            String fieldName,
            Value value
    ) {
        if (value == null) {
            return unrestricted();
        }
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.lessThanOrEqualTo(
                root.get(fieldName),
                value
        );
    }

    public static <Entity, Value> Specification<Entity> inIfNotEmpty(
            String fieldName,
            Collection<Value> values
    ) {
        if (CollectionUtils.isEmpty(values)) {
            return unrestricted();
        }
        List<Value> list = new ArrayList<>(values);
        if (list.size() <= IN_CLAUSE_CHUNK_SIZE) {
            return (root, criteriaQuery, criteriaBuilder) -> root.get(fieldName).in(list);
        }
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.or(
                chunkPredicates(root, fieldName, list)
        );
    }

    public static <Entity> Specification<Entity> fieldStartsWithNormalized(
            String fieldName,
            String sanitizedSearchQuery
    ) {
        String normalized = SearchQueryNormalizer.normalizeOrEmpty(sanitizedSearchQuery);
        if (normalized.isEmpty()) {
            return unrestricted();
        }
        String escapedQuery = LikeEscaper.escape(normalized);
        return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(
                        criteriaBuilder.function(
                                "translate",
                                String.class,
                                root.get(fieldName),
                                criteriaBuilder.literal("ёЁ"),
                                criteriaBuilder.literal("еЕ")
                        )
                ),
                escapedQuery + "%",
                '\\'
        );
    }

    public static <Entity> Specification<Entity> arrayContainsAny(
            String fieldName,
            Collection<String> values
    ) {
        if (CollectionUtils.isEmpty(values)) {
            return unrestricted();
        }
        List<String> sanitized = values.stream()
                .filter(SearchQueryNormalizer::isMeaningful)
                .map(SearchQueryNormalizer::normalizeOrEmpty)
                .distinct()
                .toList();
        if (sanitized.isEmpty()) {
            return unrestricted();
        }

        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String value : sanitized) {
                Predicate predicate = criteriaBuilder.like(
                        criteriaBuilder.lower(
                                criteriaBuilder.function(
                                        "array_to_string",
                                        String.class,
                                        root.get(fieldName),
                                        criteriaBuilder.literal(",")
                                )
                        ),
                        "%" + LikeEscaper.escape(value) + "%",
                        '\\'
                );
                predicates.add(predicate);
            }
            return criteriaBuilder.or(predicates.toArray(new Predicate[0]));
        };
    }

    public static <Entity, Value> Specification<Entity> notInIfNotEmpty(
            String fieldName,
            Collection<Value> values
    ) {
        if (CollectionUtils.isEmpty(values)) {
            return unrestricted();
        }
        List<Value> list = new ArrayList<>(values);
        if (list.size() <= IN_CLAUSE_CHUNK_SIZE) {
            return (root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.not(root.get(fieldName).in(list));
        }
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.not(criteriaBuilder.or(chunkPredicates(root, fieldName, list)));
    }

    public static <Entity> Specification<Entity> notNullAndNotIn(
            String fieldName,
            Collection<String> values
    ) {
        if (CollectionUtils.isEmpty(values)) {
            return (root, criteriaQuery, criteriaBuilder) -> criteriaBuilder.isNotNull(root.get(fieldName));
        }
        List<String> list = new ArrayList<>(values);
        if (list.size() <= IN_CLAUSE_CHUNK_SIZE) {
            return (root, criteriaQuery, criteriaBuilder) ->
                    criteriaBuilder.and(
                            criteriaBuilder.isNotNull(root.get(fieldName)),
                            criteriaBuilder.not(root.get(fieldName).in(list))
                    );
        }
        return (root, criteriaQuery, criteriaBuilder) ->
                criteriaBuilder.and(
                        criteriaBuilder.isNotNull(root.get(fieldName)),
                        criteriaBuilder.not(criteriaBuilder.or(chunkPredicates(root, fieldName, list)))
                );
    }

    private static <Value> Predicate[] chunkPredicates(Root<?> root,
                                                       String fieldName,
                                                       List<Value> values) {
        List<Predicate> predicates = new ArrayList<>();
        for (int index = 0; index < values.size(); index += IN_CLAUSE_CHUNK_SIZE) {
            List<Value> chunk = values.subList(index,
                    Math.min(index + IN_CLAUSE_CHUNK_SIZE, values.size())
            );
            predicates.add(root.get(fieldName).in(chunk));
        }
        return predicates.toArray(Predicate[]::new);
    }
}