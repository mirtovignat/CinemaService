package com.example.demo.validation.annotation;

import com.example.demo.validation.validator.ProfileFieldValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ProfileFieldValidator.class)
public @interface ValidProfileField {
    String message() default "Некорректное значение поля";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}