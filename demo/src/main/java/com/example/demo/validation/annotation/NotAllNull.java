package com.example.demo.validation.annotation;

import com.example.demo.validation.validator.NotAllNullValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.PACKAGE)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = NotAllNullValidator.class)
public @interface NotAllNull {
    String message() default "Все поля объекта не могут быть null одновременно";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}