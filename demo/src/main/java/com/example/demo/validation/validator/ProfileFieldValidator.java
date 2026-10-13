package com.example.demo.validation.validator;

import com.example.demo.dto.authorize.Identifier;
import com.example.demo.dto.user.ProfileFieldUpdateDTO;
import com.example.demo.util.PhoneNormalizer;
import com.example.demo.validation.annotation.ValidProfileField;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProfileFieldValidator implements ConstraintValidator<ValidProfileField, ProfileFieldUpdateDTO> {

    private final ValidEmailDomainValidator emailDomainValidator;

    @Override
    public boolean isValid(ProfileFieldUpdateDTO profileFieldUpdateDTO, ConstraintValidatorContext constraintValidatorContext) {
        if (profileFieldUpdateDTO == null) {
            return true;
        }
        Identifier identifier = profileFieldUpdateDTO.identifier();
        String field = profileFieldUpdateDTO.field();
        if (field == null) {
            return true;
        }

        String trimmed = field.trim();

        switch (identifier) {
            case EMAIL -> {
                if (trimmed.isEmpty()) {
                    setError(constraintValidatorContext, "field", "Email не может быть пустым");
                    return false;
                }
                String emailRegex = "^[a-zA-Z0-9+_.-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
                if (!trimmed.matches(emailRegex)) {
                    setError(constraintValidatorContext, "field", "Некорректный формат email. Допустимы только латинские буквы, цифры, _ . -");
                    return false;
                }
                String[] parts = trimmed.split("@");
                if (parts.length != 2) {
                    setError(constraintValidatorContext, "field", "Некорректный формат email");
                    return false;
                }
                String local = parts[0];
                String domain = parts[1];
                if (!local.matches("^[a-zA-Z0-9+_.-]+$")) {
                    setError(constraintValidatorContext, "field", "Локальная часть email содержит недопустимые символы");
                    return false;
                }
                if (!domain.matches("^[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")) {
                    setError(constraintValidatorContext, "field", "Домен email содержит недопустимые символы");
                    return false;
                }
                if (!emailDomainValidator.isValid(trimmed, constraintValidatorContext)) {
                    setError(constraintValidatorContext, "field", "Почтовый домен не существует или не принимает почту");
                    return false;
                }
            }
            case PHONE -> {
                if (trimmed.isEmpty()) {
                    return true;
                }
                if (!PhoneNormalizer.isValid(trimmed)) {
                    setError(constraintValidatorContext, "field",
                            "Некорректный номер телефона. Формат: +код (XXX) XXX-XX-XX");
                    return false;
                }
            }
            case USERNAME -> {
                if (trimmed.isEmpty()) {
                    setError(constraintValidatorContext, "field", "Username не может быть пустым");
                    return false;
                }
                if (trimmed.length() < 3 || trimmed.length() > 30) {
                    setError(constraintValidatorContext, "field", "Username должен быть от 3 до 30 символов");
                    return false;
                }
                if (!trimmed.matches("^[A-Za-zА-Яа-яЁё0-9_-]+$")) {
                    setError(constraintValidatorContext, "field", "Username может содержать буквы (латиница и кириллица), цифры, _ и -");
                    return false;
                }
            }
            default -> {
                setError(constraintValidatorContext, "identifier", "Неизвестный тип поля: " + identifier);
                return false;
            }
        }

        return true;
    }

    private void setError(ConstraintValidatorContext context, String propertyNode, String message) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(propertyNode)
                .addConstraintViolation();
    }
}