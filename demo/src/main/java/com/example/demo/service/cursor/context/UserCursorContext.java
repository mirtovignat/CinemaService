package com.example.demo.service.cursor.context;

import org.jetbrains.annotations.NotNull;

public record UserCursorContext(
        @NotNull Long userId
) {
}