package com.example.demo.filter;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MovieSortResolver {

    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Direction.DESC, "releaseDate", "id");

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "price", "price",
            "duration", "duration",
            "rating", "ratingSort",
            "releaseDate", "releaseDate"
    );

    public Pageable resolve(int page, int size, String sortBy, String sortDir) {
        Sort.Direction direction = parseDirection(sortDir);

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(page, size, DEFAULT_SORT);
        }

        String field = SORTABLE_FIELDS.get(sortBy);
        if (field == null) {
            return PageRequest.of(page, size, DEFAULT_SORT);
        }

        return PageRequest.of(page, size, Sort.by(direction, field, "id"));
    }

    private Sort.Direction parseDirection(String sortDir) {
        return "asc".equalsIgnoreCase(sortDir)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
    }
}