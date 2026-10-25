package com.example.demo.util.normalizer;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class TitleNormalizer implements Normalizer {

    public static final List<String> CHARS_TO_REMOVE = List.of(
            " ", "-", ":", ",", ".", "!", "?", "—", "'", "\"", "«", "»"
    );

    @Override
    public String normalize(String input) {
        if (input == null) {
            return "";
        }
        String result = input.toLowerCase().replace('ё', 'е');
        for (String charToRemove : CHARS_TO_REMOVE) {
            result = result.replace(charToRemove, "");
        }
        return result;
    }

    public Optional<String> normalizeOptional(String input) {
        if (input == null || input.isBlank()) {
            return Optional.empty();
        }
        String normalized = normalize(input);
        if (normalized.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(normalized);
    }

    public String normalizeOrEmpty(String input) {
        return normalizeOptional(input).orElse("");
    }

    @Override
    public boolean isValid(String input) {
        return normalizeOptional(input).isPresent();
    }
}