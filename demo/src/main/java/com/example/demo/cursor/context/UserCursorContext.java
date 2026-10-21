package com.example.demo.cursor.context;

import lombok.Builder;
import org.jetbrains.annotations.NotNull;

@Builder
public record UserCursorContext(
        @NotNull Long userId
) {
}