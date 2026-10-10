package com.example.demo.validation.validator;

import com.example.demo.validation.annotation.ValidEmailDomain;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Record;
import org.xbill.DNS.*;

import java.time.Duration;
import java.util.Locale;

@Component
public class ValidEmailDomainValidator
        implements ConstraintValidator<ValidEmailDomain, String> {

    @Override
    public boolean isValid(
            String email,
            ConstraintValidatorContext constraintValidatorContext
    ) {
        if (email == null || email.isBlank()) {
            return true;
        }
        String domain = getDomain(email);
        if (domain == null) {
            return false;
        }
        return hasMailRecord(domain);
    }

    private String getDomain(String email) {
        int index = email.lastIndexOf("@");
        if (index <= 0 || index == email.length() - 1) {
            return null;
        }
        return email
                .substring(index + 1)
                .toLowerCase(Locale.ROOT);
    }

    public boolean hasMailRecord(String domain) {
        try {
            Resolver resolver = new SimpleResolver();
            resolver.setTimeout(Duration.ofSeconds(5));
            Lookup mxLookup = new Lookup(domain, Type.MX);
            mxLookup.setResolver(resolver);
            Record[] mxRecords = mxLookup.run();
            if (mxRecords != null && mxRecords.length > 0) {
                return true;
            }
            Lookup aLookup = new Lookup(domain, Type.A);
            aLookup.setResolver(resolver);
            Record[] aRecords = aLookup.run();
            return aRecords != null && aRecords.length > 0;
        } catch (Exception exception) {
            return false;
        }
    }
}