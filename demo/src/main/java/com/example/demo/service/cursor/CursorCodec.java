package com.example.demo.service.cursor;

import com.example.demo.dto.cursor.Cursor;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Component
@RequiredArgsConstructor
public class CursorCodec {

    private final ObjectMapper objectMapper;

    public String encode(Cursor cursor) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(cursor);
            return Base64.getUrlEncoder().withoutPadding().encodeToString(jsonBytes);
        } catch (Exception exception) {
            throw new IllegalStateException("Cursor encoding failed", exception);
        }
    }

    public Cursor decode(String rawCursor) {
        if (rawCursor == null || rawCursor.isBlank()) {
            return null;
        }
        try {
            byte[] jsonBytes = Base64.getUrlDecoder().decode(rawCursor);
            Cursor cursor = objectMapper.readValue(jsonBytes, Cursor.class);
            if (cursor == null || cursor.value() == null || cursor.value().isBlank()) {
                throw BusinessException.of(ErrorCode.INVALID_CURSOR);
            }
            return cursor;
        } catch (BusinessException businessException) {
            throw businessException;
        } catch (Exception exception) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }
    }
}