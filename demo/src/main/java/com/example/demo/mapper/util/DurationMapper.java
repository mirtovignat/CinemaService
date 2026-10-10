package com.example.demo.mapper.util;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.Duration;

@Mapper(componentModel = "spring")
public interface DurationMapper {

    @Named("durationToString")
    default String durationToString(Duration duration) {
        if (duration == null) return null;
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }
}