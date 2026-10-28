package com.example.demo.mapper.util;

import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

public final class DateTimeMapperUtil {

    @Named("toUtcIso")
    public static String toUtcIso(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
    }
}