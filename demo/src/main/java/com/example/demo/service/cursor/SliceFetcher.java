package com.example.demo.service.cursor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@FunctionalInterface
public interface SliceFetcher<Entity> {
    Slice<Entity> fetch(Long userId,
                        boolean hasCursor,
                        Long cursorId,
                        String query,
                        Pageable pageable);
}