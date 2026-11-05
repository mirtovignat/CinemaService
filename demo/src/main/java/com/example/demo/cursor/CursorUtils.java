package com.example.demo.cursor;

import com.example.demo.dto.request.CursorPageRequest;
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

    public static <Item> Slice<Item> buildLimitedSlice(List<Item> fetchedItems, int pageSize) {
        boolean hasMore = fetchedItems.size() > pageSize;

        List<Item> content;
        if (hasMore) {
            content = fetchedItems.subList(0, pageSize);
        } else {
            content = fetchedItems;
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