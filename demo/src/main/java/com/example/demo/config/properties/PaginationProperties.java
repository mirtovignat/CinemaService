package com.example.demo.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.pagination")
public record PaginationProperties(
        @Min(1) int defaultSize,
        @Min(1) int genreDefaultSize,
        @Min(1) int maxSize
) {
}