package com.example.demo.config.properties;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.suggestions")
public record SuggestionsProperties(
        @Min(1) int catalogLimit,
        @Min(1) int dropdownLimit
) {
}