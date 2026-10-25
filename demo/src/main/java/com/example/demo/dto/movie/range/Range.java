package com.example.demo.dto.movie.range;

import lombok.Builder;

@Builder
public record Range<Value>(
        Value min,
        Value max
) {

    public static <Value> Range<Value> of(Value min, Value max) {
        return Range.<Value>builder()
                .min(min)
                .max(max)
                .build();
    }
}