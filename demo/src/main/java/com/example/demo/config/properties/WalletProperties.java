package com.example.demo.config.properties;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

@Validated
@ConfigurationProperties(prefix = "app.wallet")
public record WalletProperties(
        @NotNull
        @DecimalMin("0.01")
        BigDecimal maxBalance
) {
}