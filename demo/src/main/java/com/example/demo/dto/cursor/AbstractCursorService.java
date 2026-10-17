package com.example.demo.dto.cursor;

import com.example.demo.dto.response.CursorPageResponse;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractCursorService<Entity, DTO> {

    private final CursorCodec codec;
    private final int defaultPageSize;
    private final int maxPageSize;
    private final Function<Entity, String> cursorExtractor;
    private final Function<List<Entity>, List<DTO>> mapper;

    protected AbstractCursorService(CursorCodec codec,
                                    int defaultPageSize,
                                    int maxPageSize,
                                    Function<Entity, String> cursorExtractor,
                                    Function<List<Entity>, List<DTO>> mapper) {
        this.codec = codec;
        this.defaultPageSize = defaultPageSize;
        this.maxPageSize = maxPageSize;
        this.cursorExtractor = cursorExtractor;
        this.mapper = mapper;
    }

    @Transactional(readOnly = true)
    public CursorPageResponse<DTO> page(String rawCursor, String dir, Integer size, Object... context) {
        String cursorValue = rawCursor == null ? null
                : codec.decode(rawCursor).value();

        String direction = "asc".equalsIgnoreCase(dir) ? "asc" : "desc";
        int pageSize = size == null || size <= 0 ? defaultPageSize : Math.min(size, maxPageSize);

        Slice<Entity> slice = fetchSlice(cursorValue, direction, PageRequest.of(0, pageSize + 1), context);

        List<Entity> raw = slice.getContent();
        boolean hasMore = raw.size() > pageSize;
        List<Entity> content = hasMore ? raw.subList(0, pageSize) : raw;

        String nextCursor = hasMore && !content.isEmpty()
                ? codec.encode(new Cursor(cursorExtractor.apply(content.get(content.size() - 1))))
                : null;

        return new CursorPageResponse<>(mapper.apply(content), hasMore, nextCursor, direction);
    }

    protected abstract Slice<Entity> fetchSlice(String cursorValue,
                                                String direction,
                                                PageRequest pageable,
                                                Object... context);
}