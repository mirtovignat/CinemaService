package com.example.demo.validation.validator;

import com.example.demo.configuration.properties.ValidationProperties;
import com.example.demo.dto.authorize.Identifier;
import com.example.demo.dto.user.ProfileFieldUpdateDTO;
import com.example.demo.util.PhoneNormalizer;
import com.example.demo.validation.annotation.ValidProfileField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ProfileFieldValidator
        implements ConstraintValidator<ValidProfileField, ProfileFieldUpdateDTO> {

    private static final String FIELD_PROPERTY = "field";

    private final EmailDomainPatternValidator emailDomainPatternValidator;
    private final PhoneNormalizer phoneNormalizer;
    private final ValidationProperties validationProperties;
    private final Pattern emailPattern;
    private final Pattern usernamePattern;

    public ProfileFieldValidator(EmailDomainPatternValidator emailDomainPatternValidator,
                                 PhoneNormalizer phoneNormalizer,
                                 ValidationProperties validationProperties) {
        this.emailDomainPatternValidator = emailDomainPatternValidator;
        this.phoneNormalizer = phoneNormalizer;
        this.validationProperties = validationProperties;
        this.usernamePattern = Pattern.compile(validationProperties.usernameRegex());
        this.emailPattern = Pattern.compile(validationProperties.emailRegex());
    }

    @Override
    public boolean isValid(ProfileFieldUpdateDTO profileFieldUpdateDTO,
                           ConstraintValidatorContext constraintValidatorContext) {
        if (profileFieldUpdateDTO == null) {
            return true;
        }
        String field = profileFieldUpdateDTO.field();
        if (field == null) {
            return true;
        }

        Identifier identifier = profileFieldUpdateDTO.identifier();
        String trimmedField = field.trim();

        return switch (identifier) {
            case EMAIL -> validateEmail(trimmedField, constraintValidatorContext);
            case PHONE -> validatePhone(trimmedField, constraintValidatorContext);
            case USERNAME -> validateUsername(trimmedField, constraintValidatorContext);
        };
    }

    private boolean validateEmail(String email,
                                  ConstraintValidatorContext constraintValidatorContext) {
        if (email.isEmpty()) {
            return fail(constraintValidatorContext, "Email не может быть пустым");
        }
        if (!emailPattern.matcher(email).matches()) {
            return fail(constraintValidatorContext,
                    "Некорректный формат email. Допустимы только латинские буквы, цифры, _ . -");
        }
        if (!emailDomainPatternValidator.isValid(email, constraintValidatorContext)) {
            return fail(constraintValidatorContext,
                    "Почтовый домен не существует или не принимает почту");
        }
        return true;
    }

    private boolean validatePhone(String phone,
                                  ConstraintValidatorContext constraintValidatorContext) {
        if (phone.isEmpty()) {
            return true;
        }
        if (!phoneNormalizer.isValid(phone)) {
            return fail(constraintValidatorContext,
                    "Некорректный номер телефона. Формат: +код (XXX) XXX-XX-XX");
        }
        return true;
    }

    private boolean validateUsername(String username,
                                     ConstraintValidatorContext constraintValidatorContext) {
        if (username.isEmpty()) {
            return fail(constraintValidatorContext, "Username не может быть пустым");
        }
        if (username.length() < validationProperties.usernameMinLength()
                || username.length() > validationProperties.usernameMaxLength()) {
            return fail(constraintValidatorContext,
                    "Username должен быть от " + validationProperties.usernameMinLength()
                            + " до " + validationProperties.usernameMaxLength() + " символов");
        }
        if (!usernamePattern.matcher(username).matches()) {
            return fail(constraintValidatorContext,
                    "Username может содержать буквы (латиница и кириллица), цифры, _ и -");
        }
        return true;
    }

    private boolean fail(ConstraintValidatorContext constraintValidatorContext, String message) {
        constraintValidatorContext.disableDefaultConstraintViolation();
        constraintValidatorContext.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(FIELD_PROPERTY)
                .addConstraintViolation();
        return false;
    }
}