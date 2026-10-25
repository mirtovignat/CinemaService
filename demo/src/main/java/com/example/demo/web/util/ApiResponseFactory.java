package com.example.demo.web.util;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.exception.SuccessCode;
import com.example.demo.util.MessageUtils;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@AllArgsConstructor
public class ApiResponseFactory {

    private final MessageUtils messageUtils;

    public ResponseEntity<ApiResponse> ok(SuccessCode code) {
        return ok(code, Map.of());
    }

    public ResponseEntity<ApiResponse> ok(SuccessCode code, Map<String, Object> data) {
        return ResponseEntity.ok(ApiResponse.builder()
                .code(code.name())
                .message(messageUtils.getSuccess(code))
                .data(data)
                .build());
    }
}