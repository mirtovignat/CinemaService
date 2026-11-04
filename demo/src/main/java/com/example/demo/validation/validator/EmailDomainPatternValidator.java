package com.example.demo.validation.validator;

import com.example.demo.validation.annotation.ValidEmailDomain;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class EmailDomainPatternValidator implements ConstraintValidator<ValidEmailDomain, String> {

    private static final Pattern DOMAIN_PATTERN = Pattern.compile(
            "^(?=.{1,253}$)(?!-)[A-Za-z0-9-]{1,63}(?<!-)(\\.(?!-)[A-Za-z0-9-]{1,63}(?<!-))+$"
    );

    @Override
    public boolean isValid(String email, ConstraintValidatorContext constraintValidatorContext) {
        if (email == null || email.isBlank()) {
            return true;
        }
        String domain = getDomain(email);
        if (domain == null) {
            return false;
        }
        return DOMAIN_PATTERN.matcher(domain).matches();
    }

    private String getDomain(String email) {
        int index = email.lastIndexOf("@");
        if (index <= 0 || index == email.length() - 1) {
            return null;
        }
        return email.substring(index + 1).toLowerCase(Locale.ROOT);
    }
}