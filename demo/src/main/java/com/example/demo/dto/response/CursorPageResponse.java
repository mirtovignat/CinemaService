package com.example.demo.dto.response;

import java.util.List;

public record CursorPageResponse<DataType>(
        List<DataType> content,
        boolean hasMore,
        String nextCursor,
        String sort
) {
}