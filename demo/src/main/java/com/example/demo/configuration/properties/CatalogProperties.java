package com.example.demo.configuration.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.catalog")
public record CatalogProperties(
        @Min(1) int cursorPageResponseSize
) {
}