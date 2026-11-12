package com.example.demo.service.movie.keyset;

import com.example.demo.cursor.CursorUtils;
import com.example.demo.dto.request.CursorPageRequest;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Slice;

import java.util.List;

@AllArgsConstructor
public abstract class AbstractStringKeysetService {

    private final StringKeysetPaginator paginator;
    private final int pageSize;

    public Slice<String> search(CursorPageRequest cursorPageRequest) {
        String normalizedQuery = CursorUtils.normalizeQuery(cursorPageRequest.searchQuery());
        String searchPattern = "% " + normalizedQuery + "%";

        if (CursorUtils.hasCursor(cursorPageRequest)) {
            String cursor = paginator.decodeCursor(cursorPageRequest.cursor());
            List<String> items = fetchWithCursor(searchPattern, cursor, pageSize + 1);
            return paginator.fetchCursorPage(cursorPageRequest, pageSize, items);
        }

        List<String> items = fetchFirstPage(searchPattern, pageSize + 1);
        return paginator.fetchPage(pageSize, items);
    }

    protected abstract List<String> fetchFirstPage(String searchPattern, int limit);

    protected abstract List<String> fetchWithCursor(String searchPattern, String cursor, int limit);
}