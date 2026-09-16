package com.example.demo.dto.base;
import lombok.Builder;

@Builder
public record MovieMedia(
        String posterUrl,
        String genre
) {
}