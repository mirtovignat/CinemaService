package com.example.demo.util.text;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LikeEscaper {

    public static String escape(String value) {
        if (value == null || value.isEmpty()) {
            return "";
        }
        StringBuilder stringBuilder = new StringBuilder(value.length() + 4);
        for (int index = 0; index < value.length(); index++) {
            char currentCharacter = value.charAt(index);
            if (currentCharacter == '\\' || currentCharacter == '%' || currentCharacter == '_') {
                stringBuilder.append('\\');
            }
            stringBuilder.append(currentCharacter);
        }
        return stringBuilder.toString();
    }
}