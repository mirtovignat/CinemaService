package com.example.demo.filter;

import com.example.demo.cursor.SortDirection;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class MovieSortResolver {

    private static final Sort DEFAULT_SORT = Sort.by(
            Sort.Direction.DESC,
            "releaseDate",
            "id"
    );

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "price", "price",
            "duration", "duration",
            "rating", "ratingSort",
            "releaseDate", "releaseDate"
    );

    public Pageable resolve(PageRequestContext pageRequestContext, int defaultSize) {
        int safePage = pageRequestContext.getSafePage();
        int safeSize = pageRequestContext.getSafeSize(defaultSize);

        String sortBy = pageRequestContext.sortBy();

        if (sortBy == null || sortBy.isBlank()) {
            return PageRequest.of(safePage, safeSize, DEFAULT_SORT);
        }

        String field = SORTABLE_FIELDS.get(sortBy);
        if (field == null) {
            return PageRequest.of(safePage, safeSize, DEFAULT_SORT);
        }

        Sort.Direction direction = (SortDirection.from(pageRequestContext.sortDirection()) == SortDirection.ASC)
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(
                safePage,
                safeSize,
                Sort.by(direction, field, "id")
        );
    }
}