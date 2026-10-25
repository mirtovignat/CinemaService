package com.example.demo.filter;

import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SpecificationUtils {

    private static final int IN_CLAUSE_CHUNK_SIZE = 500;

    private SpecificationUtils() {
    }

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
        String escapedQuery = escapeLike(sanitizedSearchQuery.toLowerCase().replace('ё', 'е'));
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

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
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