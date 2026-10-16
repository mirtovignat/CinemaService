package com.example.demo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class PhoneNormalizer {

    public static final int MIN_DIGITS = 11;
    public static final int MAX_DIGITS = 13;

    public static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String cleaned = phone.replaceAll("[^+0-9]", "");
        if (!cleaned.startsWith("+")) {
            return null;
        }
        if (cleaned.indexOf('+', 1) != -1) {
            return null;
        }
        String digits = cleaned.substring(1);
        if (digits.length() < MIN_DIGITS || digits.length() > MAX_DIGITS) {
            return null;
        }
        return cleaned;
    }

    public static boolean isValid(String phone) {
        return normalizePhone(phone) != null;
    }
}