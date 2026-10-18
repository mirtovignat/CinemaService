package com.example.demo.service.cursor;

import com.example.demo.service.cursor.util.CursorCodec;
import com.example.demo.service.cursor.util.KeyUtils;
import com.example.demo.service.cursor.util.UserCursorContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractUserCursorService<EntityType, DataType>
        extends AbstractCursorService<EntityType, DataType, UserCursorContext> {

    @FunctionalInterface
    public interface SliceFetcher<E> {
        Slice<E> fetch(Long userId,
                       boolean hasCursor,
                       LocalDateTime cursorAddedAt,
                       Long cursorIdentifier,
                       Pageable pageable);
    }

    private final SliceFetcher<EntityType> ascFetcher;
    private final SliceFetcher<EntityType> descFetcher;

    protected AbstractUserCursorService(CursorCodec cursorCodec,
                                        int defaultPageSize,
                                        Function<EntityType, String> cursorValueExtractor,
                                        Function<List<EntityType>, List<DataType>> contentMapper,
                                        SliceFetcher<EntityType> ascFetcher,
                                        SliceFetcher<EntityType> descFetcher) {
        super(cursorCodec, defaultPageSize, cursorValueExtractor, contentMapper);
        this.ascFetcher = ascFetcher;
        this.descFetcher = descFetcher;
    }

    @Override
    protected Slice<EntityType> fetchSlice(String cursorValue,
                                           String sortDirection,
                                           PageRequest pageRequest,
                                           UserCursorContext context) {
        Long userId = context.userId();
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorAddedAt = hasCursor ? KeyUtils.decodeTimestamp(cursorValue) : null;
        Long cursorIdentifier = hasCursor ? KeyUtils.decodeIdentifier(cursorValue) : null;

        return "asc".equals(sortDirection)
                ? ascFetcher.fetch(userId, hasCursor, cursorAddedAt, cursorIdentifier, pageRequest)
                : descFetcher.fetch(userId, hasCursor, cursorAddedAt, cursorIdentifier, pageRequest);
    }
}