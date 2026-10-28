package com.example.demo.dto.movie;

import lombok.Builder;

@Builder
public record MovieRefer(
        Long id,
        String title,
        Integer year
) {
}