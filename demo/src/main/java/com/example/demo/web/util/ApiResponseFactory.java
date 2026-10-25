package com.example.demo.web.util;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.exception.SuccessCode;
import com.example.demo.util.MessageUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ApiResponseFactory {

    private final MessageUtils messageUtils;

    public ResponseEntity<ApiResponse<Void>> ok(SuccessCode successCode) {
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .code(successCode.name())
                .message(messageUtils.getSuccess(successCode))
                .data(null)
                .build());
    }

    public <DTO> ResponseEntity<ApiResponse<DTO>> ok(SuccessCode successCode, DTO data) {
        return ResponseEntity.ok(ApiResponse.<DTO>builder()
                .code(successCode.name())
                .message(messageUtils.getSuccess(successCode))
                .data(data)
                .build());
    }
}