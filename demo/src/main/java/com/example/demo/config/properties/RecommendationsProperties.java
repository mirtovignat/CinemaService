package com.example.demo.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.recommendations")
public record RecommendationsProperties(
        @Min(1) int cursorPageResponseSize
) {
}