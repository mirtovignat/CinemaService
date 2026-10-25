package com.example.demo.util;

import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.SuccessCode;
import lombok.AllArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class MessageUtils {

    private final MessageSource messageSource;

    public String getError(ErrorCode errorCode,
                           Object... args) {
        return messageSource.getMessage(
                "error." + errorCode.name(),
                args,
                errorCode.name(),
                LocaleContextHolder.getLocale()
        );
    }

    public String getSuccess(SuccessCode successCode,
                             Object... args) {
        return messageSource.getMessage(
                "success." + successCode.name(),
                args,
                successCode.name(),
                LocaleContextHolder.getLocale()
        );
    }

}