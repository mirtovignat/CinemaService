package com.example.demo.cursor.codec;

import com.example.demo.cursor.Part;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.function.Function;

@Component
public class KeyCodec {

    public String encode(LocalDateTime timestamp, Long id) {
        Objects.requireNonNull(timestamp);
        Objects.requireNonNull(id);
        return timestamp + "_" + id;
    }

    public <T> T decode(String cursorValue,
                        Function<String, T> parser,
                        Part part) {
        int separatorIndex = cursorValue.lastIndexOf("_");
        if (separatorIndex <= 0 || separatorIndex == cursorValue.length() - 1) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        String value = part == Part.LEFT
                ? cursorValue.substring(0, separatorIndex)
                : cursorValue.substring(separatorIndex + 1);

        try {
            return parser.apply(value);
        } catch (DateTimeParseException | NumberFormatException exception) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }
    }
}