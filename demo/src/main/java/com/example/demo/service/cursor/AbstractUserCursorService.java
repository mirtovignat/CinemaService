package com.example.demo.service.cursor;

import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.IdCodec;
import com.example.demo.cursor.context.UserCursorContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.util.List;
import java.util.function.Function;

public abstract class AbstractUserCursorService<EntityType, DataType>
        extends AbstractCursorService<EntityType, DataType, UserCursorContext> {

    private final IdCodec idCodec;
    private final SliceFetcher<EntityType> ascFetcher;
    private final SliceFetcher<EntityType> descFetcher;

    protected AbstractUserCursorService(CursorCodec cursorCodec,
                                        IdCodec idCodec,
                                        int defaultPageSize,
                                        Function<EntityType, String> cursorValueExtractor,
                                        Function<List<EntityType>, List<DataType>> contentMapper,
                                        SliceFetcher<EntityType> ascFetcher,
                                        SliceFetcher<EntityType> descFetcher) {
        super(cursorCodec, defaultPageSize, cursorValueExtractor, contentMapper);
        this.idCodec = idCodec;
        this.ascFetcher = ascFetcher;
        this.descFetcher = descFetcher;
    }

    @Override
    protected Slice<EntityType> fetchSlice(String cursorValue,
                                           SortDirection sortDirection,
                                           String query,
                                           PageRequest pageRequest,
                                           UserCursorContext userCursorContext) {
        Long userId = userCursorContext.userId();
        boolean hasCursor = cursorValue != null;
        Long cursorId = hasCursor ? idCodec.decode(cursorValue) : null;

        return switch (sortDirection) {
            case ASC -> ascFetcher.fetch(userId, hasCursor, cursorId, query, pageRequest);
            case DESC -> descFetcher.fetch(userId, hasCursor, cursorId, query, pageRequest);
        };
    }
}