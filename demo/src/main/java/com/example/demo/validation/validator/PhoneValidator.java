package com.example.demo.validation.validator;

import com.example.demo.util.normalizer.PhoneNormalizer;
import com.example.demo.validation.annotation.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private final PhoneNormalizer phoneNormalizer;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return phoneNormalizer.isValid(value);
    }
}