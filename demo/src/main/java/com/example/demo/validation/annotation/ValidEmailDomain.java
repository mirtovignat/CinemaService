package com.example.demo.validation.annotation;

import com.example.demo.validation.validator.ValidEmailDomainValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({
        ElementType.FIELD,
        ElementType.PARAMETER
})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = ValidEmailDomainValidator.class)
public @interface ValidEmailDomain {

    String message() default "Данный @email не существует";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}