package com.example.demo.service.cursor;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class KeyUtils {

    public static String encodeTimestampAndIdentifier(LocalDateTime timestamp,
                                                      Long identifier) {
        return timestamp.toString() + "_" + identifier;
    }

    public static LocalDateTime decodeTimestamp(String cursorValue) {
        int separatorIndex = cursorValue.lastIndexOf('_');
        if (separatorIndex <= 0 || separatorIndex == cursorValue.length() - 1) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }
        try {
            return LocalDateTime.parse(cursorValue.substring(0, separatorIndex));
        } catch (Exception exception) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }
    }

    public static Long decodeIdentifier(String cursorValue) {
        int separatorIndex = cursorValue.lastIndexOf('_');
        if (separatorIndex <= 0 || separatorIndex == cursorValue.length() - 1) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }
        try {
            return Long.parseLong(cursorValue.substring(separatorIndex + 1));
        } catch (Exception exception) {
            throw BusinessException.of(ErrorCode.INVALID_CURSOR);
        }
    }
}