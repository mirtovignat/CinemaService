package com.example.demo.configuration.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.reviews")
public record ReviewsProperties(
        @Min(1) int cursorPageResponseSize
) {
}