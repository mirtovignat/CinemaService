package com.example.demo.config.properties;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.validation")
public record ValidationProperties(
        @Min(1) int usernameMinLength,
        @Min(1) int usernameMaxLength,
        @NotBlank String usernameRegex,
        @NotBlank String emailRegex
) {
}