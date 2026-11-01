package com.example.demo.dto.wallet;

import com.example.demo.util.text.StringSanitizer;

import java.math.BigDecimal;
import java.util.Locale;

public record WalletForOwnerViewDTO(
        String phone,
        String fullName,
        BigDecimal balance,
        BigDecimal maxBalance,
        String currencyCode,
        String username
) {
    public String getFirstUsernameLetter() {
        return StringSanitizer.trimToNull(username)
                .map(usernamePart -> usernamePart.substring(0, usernamePart.offsetByCodePoints(0, 1)).toUpperCase(Locale.ROOT))
                .orElse("U");
    }
}