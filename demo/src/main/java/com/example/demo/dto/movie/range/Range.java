package com.example.demo.dto.movie.range;

import lombok.Builder;

@Builder
public record Range<T>(T min, T max) {

    public static <T> Range<T> of(T min, T max) {
        return Range.<T>builder()
                .min(min)
                .max(max)
                .build();
    }
}