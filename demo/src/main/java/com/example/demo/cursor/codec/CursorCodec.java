package com.example.demo.cursor.codec;

import com.example.demo.cursor.Cursor;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import lombok.NonNull;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Component
public class CursorCodec {

    public String encode(@NonNull Cursor cursor) {
        byte[] encodedBytes = cursor.value()
                .getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(encodedBytes);
    }

    public Cursor decode(@NonNull String cursorValue) {
        byte[] valueBytes;
        try {
            valueBytes = Base64.getUrlDecoder()
                    .decode(cursorValue);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        String value = new String(valueBytes, StandardCharsets.UTF_8);

        if (value.isBlank()) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        return Cursor.builder()
                .value(value)
                .build();
    }
}