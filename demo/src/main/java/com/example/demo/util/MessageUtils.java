package com.example.demo.util;

import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.SuccessCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageUtils {

    private final MessageSource messageSource;

    public String getError(ErrorCode code, Object... args) {
        return messageSource.getMessage(
                "error." + code.name(),
                args,
                code.name(),
                LocaleContextHolder.getLocale()
        );
    }

    public String getSuccess(SuccessCode code, Object... args) {
        return messageSource.getMessage(
                "success." + code.name(),
                args,
                code.name(),
                LocaleContextHolder.getLocale()
        );
    }

    public String getMessage(String key, Object... args) {
        return messageSource.getMessage(key, args, key, LocaleContextHolder.getLocale());
    }
}