package com.example.demo.exception;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Object[] args;

    private BusinessException(ErrorCode errorCode, Object[] args) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.args = args;
    }

    public static BusinessException of(ErrorCode errorCode, Object... args) {
        return BusinessException.builder()
                .errorCode(errorCode)
                .args(args)
                .build();
    }
}