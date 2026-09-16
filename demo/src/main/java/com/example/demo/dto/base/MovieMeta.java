package com.example.demo.dto.base;

import lombok.Builder;

@Builder
public record MovieMeta(
        String director,
        String formattedDuration,
        String description,
        Long ratingsCount
) {
}