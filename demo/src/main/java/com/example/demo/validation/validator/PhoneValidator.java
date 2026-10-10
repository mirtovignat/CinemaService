package com.example.demo.validation.validator;

import com.example.demo.util.PhoneNormalizer;
import com.example.demo.validation.annotation.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return PhoneNormalizer.isValid(value);
    }
}