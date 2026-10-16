package com.example.demo.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class TitleNormalizer {

    public static final List<String> CHARS_TO_REMOVE = List.of(
            " ", "-", ":", ",", ".", "!", "?", "—", "'", "\"", "«", "»"
    );

    public static String normalize(String input) {
        if (input == null) {
            return "";
        }
        String result = input.toLowerCase().replace('ё', 'е');
        for (String charToRemove : CHARS_TO_REMOVE) {
            result = result.replace(charToRemove, "");
        }
        return result;
    }
}