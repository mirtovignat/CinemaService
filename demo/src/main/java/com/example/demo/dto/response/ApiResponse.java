package com.example.demo.dto.response;

import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.SuccessCode;
import lombok.Builder;

import java.util.Map;

@Builder
public record ApiResponse(
        String message,
        String code,
        Map<String, Object> data
) {
    public static ApiResponse success(SuccessCode code, String message) {
        return ApiResponse.builder()
                .code(code.name())
                .message(message)
                .data(Map.of())
                .build();
    }

    public static ApiResponse error(ErrorCode code, String message) {
        return ApiResponse.builder()
                .code(code.name())
                .message(message)
                .data(Map.of())
                .build();
    }
}