package com.example.demo.dto.cursor;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Base64;

public class CursorCodec {

    private final ObjectMapper mapper;

    public CursorCodec(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public String encode(Cursor cursor) {
        try {
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mapper.writeValueAsBytes(cursor));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public Cursor decode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        try {
            return mapper.readValue(
                    Base64.getUrlDecoder().decode(raw), Cursor.class);
        } catch (Exception e) {
            throw com.example.demo.exception.BusinessException
                    .of(com.example.demo.exception.ErrorCode.INVALID_CURSOR);
        }
    }
}