package com.example.demo.service.cursor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.time.LocalDateTime;

@FunctionalInterface
public interface SliceFetcher<Entity> {

    Slice<Entity> fetch(Long userId,
                        boolean hasCursor,
                        LocalDateTime cursorAddedAt,
                        Long cursorIdentifier,
                        String query,
                        Pageable pageable);
}