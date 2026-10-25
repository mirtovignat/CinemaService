package com.example.demo.service.cursor;

import com.example.demo.cursor.Cursor;
import com.example.demo.cursor.CursorUtils;
import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.dto.response.CursorPageResponse;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.function.Function;

@AllArgsConstructor
public abstract class AbstractCursorService<EntityType, DataType, ContextType>
        implements CursorPageService<ContextType, DataType> {

    private final CursorCodec cursorCodec;
    private final int defaultPageSize;
    private final Function<EntityType, String> cursorValueExtractor;
    private final Function<List<EntityType>, List<DataType>> contentMapper;

    @Transactional(readOnly = true)
    public CursorPageResponse<DataType> page(String rawCursor,
                                             SortDirection sortDirection,
                                             String query,
                                             ContextType contextType) {

        String cursorValue = StringUtils.hasText(rawCursor)
                ? cursorCodec.decode(rawCursor).value()
                : null;

        String normalizedQuery = CursorUtils.normalizeQuery(query);

        Slice<EntityType> slice = fetchSlice(
                cursorValue,
                sortDirection,
                normalizedQuery,
                PageRequest.of(0, defaultPageSize + 1),
                contextType
        );

        List<EntityType> rawContent = slice.getContent();
        boolean hasMore = rawContent.size() > defaultPageSize;

        List<EntityType> pageContent = hasMore
                ? rawContent.subList(0, defaultPageSize)
                : rawContent;

        String nextCursor = null;
        if (hasMore && !pageContent.isEmpty()) {
            EntityType lastEntity = pageContent.get(pageContent.size() - 1);
            String lastCursorValue = cursorValueExtractor.apply(lastEntity);
            nextCursor = cursorCodec.encode(Cursor.builder().value(lastCursorValue).build());
        }

        return CursorPageResponse.<DataType>builder()
                .items(contentMapper.apply(pageContent))
                .hasMore(hasMore)
                .nextCursor(nextCursor)
                .sort(sortDirection.lowercase())
                .build();
    }

    protected abstract Slice<EntityType> fetchSlice(String cursorValue,
                                                    SortDirection sortDirection,
                                                    String query,
                                                    PageRequest pageRequest,
                                                    ContextType contextType);
}