package com.example.demo.cursor;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

public final class CursorUtils {

    private static final String EMPTY_STRING = "";

    private CursorUtils() {
    }

    public static boolean hasCursor(CursorPageRequest cursorPageRequest) {
        String cursor = cursorPageRequest.cursor();
        return cursor != null && !cursor.isBlank();
    }

    public static String safeQuery(CursorPageRequest cursorPageRequest) {
        String query = cursorPageRequest.query();
        return query != null ? query : EMPTY_STRING;
    }

    public static String safeCursor(CursorPageRequest cursorPageRequest) {
        return hasCursor(cursorPageRequest) ? cursorPageRequest.cursor() : EMPTY_STRING;
    }

    public static <T> Slice<T> buildLimitedSlice(List<T> rows, int pageSize) {
        boolean hasMore = rows.size() > pageSize;
        List<T> content = hasMore ? rows.subList(0, pageSize) : rows;
        return new SliceImpl<>(content, PageRequest.of(0, pageSize), hasMore);
    }

    public static String normalizeQuery(String query) {
        if (query == null || query.isBlank()) {
            return null;
        }
        String trimmed = query.trim().toLowerCase().replace('ё', 'е');
        if (trimmed.isBlank()) {
            return null;
        }
        return escapeLike(trimmed);
    }

    private static String escapeLike(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}