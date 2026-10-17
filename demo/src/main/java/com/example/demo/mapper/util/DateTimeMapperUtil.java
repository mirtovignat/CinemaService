package com.example.demo.mapper.util;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Mapper(componentModel = "spring")
public interface DateTimeMapperUtil {

    @Named("toUtcIso")
    static String toUtcIso(LocalDateTime value) {
        return value == null ? null : value.toInstant(ZoneOffset.UTC).toString();
    }
}