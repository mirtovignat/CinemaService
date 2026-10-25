package com.example.demo.dto.response;

import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.SuccessCode;
import lombok.Builder;

@Builder
public record ApiResponse<DTO>(
        String message,
        String code,
        DTO data
) {
    public static <DTO> ApiResponse<DTO> success(SuccessCode successCode, String message, DTO data) {
        return ApiResponse.<DTO>builder()
                .code(successCode.name())
                .message(message)
                .data(data)
                .build();
    }

    public static <DTO> ApiResponse<DTO> error(ErrorCode errorCode, String message) {
        return ApiResponse.<DTO>builder()
                .code(errorCode.name())
                .message(message)
                .data(null)
                .build();
    }
}