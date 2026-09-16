package com.example.demo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PhoneNormalizer {

    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }

        String cleaned = phone.replaceAll("[^+0-9]", "");

        if (!cleaned.startsWith("+")) {
            return null;
        }

        String digits = cleaned.replace("+", "");

        if (digits.length() < 10 || digits.length() > 15) {
            return null;
        }

        return cleaned;
    }
}