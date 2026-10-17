package com.example.demo.service.cursor;

import com.example.demo.dto.cursor.Cursor;
import com.example.demo.dto.response.CursorPageResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
public abstract class AbstractCursorService<EntityType, DataType> {

    private final CursorCodec cursorCodec;
    private final int defaultPageSize;
    private final int maximumPageSize;
    private final Function<EntityType, String> cursorValueExtractor;
    private final Function<List<EntityType>, List<DataType>> contentMapper;

    @Transactional(readOnly = true)
    public CursorPageResponse<DataType> page(String rawCursor,
                                             String sortDirection,
                                             Integer requestedPageSize,
                                             Long... contextValues) {
        String cursorValue = null;
        if (rawCursor != null && !rawCursor.isBlank()) {
            Cursor decodedCursor = cursorCodec.decode(rawCursor);
            cursorValue = decodedCursor != null ? decodedCursor.value() : null;
        }

        String normalizedSortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";
        int effectivePageSize = resolvePageSize(requestedPageSize);

        Slice<EntityType> slice = fetchSlice(
                cursorValue,
                normalizedSortDirection,
                PageRequest.of(0, effectivePageSize + 1),
                contextValues);

        List<EntityType> rawContent = slice.getContent();
        boolean hasMore = rawContent.size() > effectivePageSize;
        List<EntityType> pageContent = hasMore
                ? rawContent.subList(0, effectivePageSize)
                : rawContent;

        String nextCursor = null;
        if (hasMore && !pageContent.isEmpty()) {
            EntityType lastEntity = pageContent.get(pageContent.size() - 1);
            String lastCursorValue = cursorValueExtractor.apply(lastEntity);
            if (lastCursorValue != null) {
                nextCursor = cursorCodec.encode(new Cursor(lastCursorValue));
            }
        }

        return new CursorPageResponse<>(
                contentMapper.apply(pageContent),
                hasMore,
                nextCursor,
                normalizedSortDirection);
    }

    protected abstract Slice<EntityType> fetchSlice(String cursorValue,
                                                    String sortDirection,
                                                    PageRequest pageRequest,
                                                    Long... contextValues);

    private int resolvePageSize(Integer requestedPageSize) {
        if (requestedPageSize == null || requestedPageSize <= 0) {
            return defaultPageSize;
        }
        return Math.min(requestedPageSize, maximumPageSize);
    }
}