package com.example.demo.util;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;

import java.util.Comparator;

public final class RangeValidator {

    private RangeValidator() {
    }

    public static <T> void validate(T from, T to, Comparator<T> comparator) {
        if (from != null && to != null && comparator.compare(from, to) > 0) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
    }
}