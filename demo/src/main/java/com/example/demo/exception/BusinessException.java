package com.example.demo.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final ErrorCode code;
    private final Object[] args;

    private BusinessException(ErrorCode code, Object... args) {
        super(code.name());
        this.code = code;
        this.args = args;
    }

    public static BusinessException of(ErrorCode code, Object... args) {
        return new BusinessException(code, args);
    }
}