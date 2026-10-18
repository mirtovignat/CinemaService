package com.example.demo.service.cursor.util;

import org.jetbrains.annotations.NotNull;

public record UserCursorContext(
        @NotNull Long userId
) {
}