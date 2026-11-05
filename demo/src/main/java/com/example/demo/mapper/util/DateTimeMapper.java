package com.example.demo.mapper.util;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Component
public class DateTimeMapper {

    @Named("localDateTimeToIso")
    public String toUtcIso(LocalDateTime value) {
        if (value == null) {
            return null;
        }
        return value.toInstant(ZoneOffset.UTC).toString();
    }
}