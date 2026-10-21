package com.example.demo.service.cursor;

import com.example.demo.cursor.Part;
import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.KeyCodec;
import com.example.demo.cursor.context.UserCursorContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Function;

public abstract class AbstractUserCursorService<EntityType, DataType>
        extends AbstractCursorService<EntityType, DataType, UserCursorContext> {

    private final KeyCodec keyCodec;
    private final SliceFetcher<EntityType> ascFetcher;
    private final SliceFetcher<EntityType> descFetcher;

    protected AbstractUserCursorService(CursorCodec cursorCodec,
                                        KeyCodec keyCodec,
                                        int defaultPageSize,
                                        Function<EntityType, String> cursorValueExtractor,
                                        Function<List<EntityType>, List<DataType>> contentMapper,
                                        SliceFetcher<EntityType> ascFetcher,
                                        SliceFetcher<EntityType> descFetcher) {
        super(cursorCodec, defaultPageSize, cursorValueExtractor, contentMapper);
        this.keyCodec = keyCodec;
        this.ascFetcher = ascFetcher;
        this.descFetcher = descFetcher;
    }

    @Override
    protected Slice<EntityType> fetchSlice(String cursorValue,
                                           SortDirection sortDirection,
                                           PageRequest pageRequest,
                                           UserCursorContext userCursorContext) {
        Long userId = userCursorContext.userId();
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorAddedAt = hasCursor
                ? keyCodec.decode(cursorValue, LocalDateTime::parse, Part.LEFT)
                : null;
        Long cursorIdentifier = hasCursor
                ? keyCodec.decode(cursorValue, Long::parseLong, Part.RIGHT)
                : null;

        return switch (sortDirection) {
            case ASC -> ascFetcher.fetch(
                    userId, hasCursor, cursorAddedAt, cursorIdentifier, pageRequest);
            case DESC -> descFetcher.fetch(
                    userId, hasCursor, cursorAddedAt, cursorIdentifier, pageRequest);
        };
    }
}