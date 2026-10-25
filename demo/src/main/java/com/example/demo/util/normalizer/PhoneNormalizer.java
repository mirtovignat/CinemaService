package com.example.demo.util.normalizer;

import org.springframework.stereotype.Component;

@Component
public class PhoneNormalizer implements Normalizer {

    @Override
    public String normalize(String phone) {
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
        if (digits.length() < 11 || digits.length() > 13) {
            return null;
        }
        return cleaned;
    }

    @Override
    public boolean isValid(String phone) {
        return normalize(phone) != null;
    }
}