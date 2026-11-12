package com.example.demo.service.movie.keyset;

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
            int pageSize,
            List<String> firstPageItems
    ) {
        return CursorUtils.buildLimitedSlice(firstPageItems, pageSize);
    }

    public Slice<String> fetchCursorPage(
            CursorPageRequest cursorPageRequest,
            int pageSize,
            List<String> cursorPageItems
    ) {
        return CursorUtils.buildLimitedSlice(cursorPageItems, pageSize);
    }

    public String decodeCursor(String rawCursor) {
        return cursorCodec.decode(rawCursor).value();
    }
}