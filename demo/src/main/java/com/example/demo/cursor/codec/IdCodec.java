package com.example.demo.cursor.codec;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import org.springframework.stereotype.Component;

@Component
public class IdCodec {
    public String encode(Long id) {
        if (id == null) throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        return String.valueOf(id);
    }

    public Long decode(String cursorValue) {
        try {
            return Long.parseLong(cursorValue);
        } catch (NumberFormatException e) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }
    }
}