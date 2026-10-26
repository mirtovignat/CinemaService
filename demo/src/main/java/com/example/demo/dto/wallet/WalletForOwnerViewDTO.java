package com.example.demo.dto.wallet;

import java.math.BigDecimal;

public record WalletForOwnerViewDTO(
        String phone,
        String fullName,
        BigDecimal balance,
        BigDecimal maxBalance,
        String currencyCode,
        String username
) {
    public String getFirstUsernameLetter() {
        String username = username();
        if (username == null || username.isBlank()) {
            return "U";
        }
        String firstChar = username.substring(0, username.offsetByCodePoints(0, 1));
        return firstChar.toUpperCase(java.util.Locale.ROOT);
    }
}