package com.example.demo.util.validation;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;

import java.math.BigDecimal;

public final class Amounts {

    public static void requirePositive(BigDecimal amount, ErrorCode errorCode) {
        if (amount == null || amount.signum() <= 0) {
            throw BusinessException.of(errorCode);
        }
    }

    public static void requirePositive(BigDecimal amount) {
        requirePositive(amount, ErrorCode.VALIDATION_ERROR);
    }
}