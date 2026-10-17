package com.example.demo.service.cursor.context;

import org.jetbrains.annotations.NotNull;

public record ReviewCursorContext(
        @NotNull Long movieId,
        Long viewerId
) {
}