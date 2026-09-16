package com.example.demo.validation.validator;

import com.example.demo.validation.annotation.NotAllNull;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.stream.Stream;

public class NotAllNullValidator implements ConstraintValidator<NotAllNull, Object> {

    @Override
    public boolean isValid(Object object, ConstraintValidatorContext context) {
        if (object == null) {
            return true;
        }
        if (!object.getClass().getPackage().isAnnotationPresent(NotAllNull.class)) {
            return true;
        }
        return Stream.of(object.getClass().getDeclaredFields())
                .anyMatch(field -> {
                    try {
                        field.setAccessible(true);
                        return field.get(object) != null;
                    } catch (IllegalAccessException e) {
                        return false;
                    }
                });
    }
}