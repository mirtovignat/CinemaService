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
        byte[] raw = cursor.value()
                .getBytes(StandardCharsets.UTF_8);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(raw);
    }

    public Cursor decode(@NonNull String rawCursor) {
        byte[] raw;
        try {
            raw = Base64.getUrlDecoder()
                    .decode(rawCursor);
        } catch (IllegalArgumentException illegalArgumentException) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        String value = new String(raw, StandardCharsets.UTF_8);

        if (value.isBlank()) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }

        return Cursor.builder()
                .value(value)
                .build();
    }
}