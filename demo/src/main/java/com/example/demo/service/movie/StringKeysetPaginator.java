package com.example.demo.service.movie;

import com.example.demo.cursor.CursorUtils;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.dto.request.CursorPageRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Component;

import java.util.List;

@AllArgsConstructor
@Component
public class StringKeysetPaginator {

    private final CursorCodec cursorCodec;

    public Slice<String> fetchPage(
            CursorPageRequest cursorPageRequest,
            int pageSize,
            KeysetFetcher keysetFetcher
    ) {
        String normalizedQuery = CursorUtils.normalizeQuery(cursorPageRequest.searchQuery());

        boolean hasCursor = CursorUtils.hasCursor(cursorPageRequest);

        String cursor;
        if (hasCursor) {
            cursor = cursorCodec.decode(cursorPageRequest.cursor()).value();
        } else {
            cursor = "";
        }

        List<String> fetchedItems = keysetFetcher.fetch(normalizedQuery, hasCursor, cursor, pageSize + 1);

        return CursorUtils.buildLimitedSlice(fetchedItems, pageSize);
    }

}