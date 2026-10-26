package com.example.demo.validation.validator;

import com.example.demo.util.PhoneNormalizer;
import com.example.demo.validation.annotation.ValidPhone;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class PhoneValidator implements ConstraintValidator<ValidPhone, String> {

    private final PhoneNormalizer phoneNormalizer;

    @Override
    public boolean isValid(String value, ConstraintValidatorContext constraintValidatorContext) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return phoneNormalizer.isValid(value);
    }
}