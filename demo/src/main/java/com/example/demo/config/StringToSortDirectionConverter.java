package com.example.demo.config;

import com.example.demo.cursor.SortDirection;
import org.jetbrains.annotations.NotNull;
import org.springframework.core.convert.converter.Converter;
import org.springframework.stereotype.Component;

@Component
public class StringToSortDirectionConverter implements Converter<String, SortDirection> {

    @Override
    public SortDirection convert(@NotNull String source) {
        return SortDirection.from(source);
    }
}