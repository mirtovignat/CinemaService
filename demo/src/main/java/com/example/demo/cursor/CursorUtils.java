package com.example.demo.cursor;

import com.example.demo.util.text.SearchQueryNormalizer;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;

public final class CursorUtils {

    public static boolean hasCursor(CursorPageRequest cursorPageRequest) {
        String cursor = cursorPageRequest.cursor();
        return cursor != null && !cursor.isBlank();
    }

    public static <T> Slice<T> buildLimitedSlice(List<T> rows, int pageSize) {
        boolean hasMore = rows.size() > pageSize;

        List<T> content;
        if (hasMore) {
            content = rows.subList(0, pageSize);
        } else {
            content = rows;
        }

        return new SliceImpl<>(
                content,
                PageRequest.of(0, pageSize),
                hasMore
        );
    }

    public static String normalizeQuery(String query) {
        return SearchQueryNormalizer.normalizeAndEscape(query);
    }
}