package com.example.demo.util;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class SpecificationUtils {

    private static final int IN_CLAUSE_CHUNK_SIZE = 500;

    private SpecificationUtils() {
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> gteIfNotNull(
            String fieldName, Y value) {
        if (value == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get(fieldName), value);
    }

    public static <T, Y extends Comparable<? super Y>> Specification<T> lteIfNotNull(
            String fieldName, Y value) {
        if (value == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get(fieldName), value);
    }

    public static <T, Y> Specification<T> inIfNotEmpty(
            String fieldName, Collection<Y> values) {
        if (values == null || values.isEmpty()) {
            return Specification.unrestricted();
        }
        List<Y> list = new ArrayList<>(values);
        if (list.size() <= IN_CLAUSE_CHUNK_SIZE) {
            return (root, query, cb) -> root.get(fieldName).in(list);
        }
        return (root, query, cb) -> cb.or(chunkPredicates(root, fieldName, list, cb));
    }

    private static <Y> Predicate[] chunkPredicates(Root<?> root,
                                                    String fieldName,
                                                    List<Y> values,
                                                    CriteriaBuilder cb) {
        List<Predicate> predicates = new ArrayList<>();
        for (int index = 0; index < values.size(); index += IN_CLAUSE_CHUNK_SIZE) {
            List<Y> chunk = values.subList(index,
                    Math.min(index + IN_CLAUSE_CHUNK_SIZE, values.size()));
            predicates.add(root.get(fieldName).in(chunk));
        }
        return predicates.toArray(new Predicate[0]);
    }
}