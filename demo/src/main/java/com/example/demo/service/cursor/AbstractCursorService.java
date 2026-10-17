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
public abstract class AbstractCursorService<EntityType, DataType, ContextType> {

    private final CursorCodec cursorCodec;
    private final int defaultPageSize;
    private final Function<EntityType, String> cursorValueExtractor;
    private final Function<List<EntityType>, List<DataType>> contentMapper;

    @Transactional(readOnly = true)
    public CursorPageResponse<DataType> page(String rawCursor,
                                             String sortDirection,
                                             ContextType context) {
        String cursorValue = null;
        if (rawCursor != null && !rawCursor.isBlank()) {
            Cursor decodedCursor = cursorCodec.decode(rawCursor);
            cursorValue = decodedCursor != null ? decodedCursor.value() : null;
        }

        String normalizedSortDirection = "asc".equalsIgnoreCase(sortDirection) ? "asc" : "desc";

        Slice<EntityType> slice = fetchSlice(
                cursorValue,
                normalizedSortDirection,
                PageRequest.of(0, defaultPageSize + 1),
                context);

        List<EntityType> rawContent = slice.getContent();
        boolean hasMore = rawContent.size() > defaultPageSize;
        List<EntityType> pageContent = hasMore
                ? rawContent.subList(0, defaultPageSize)
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
                                                    ContextType context);
}