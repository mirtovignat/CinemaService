package com.example.demo.mapper.util;

import org.mapstruct.Mapper;
import org.mapstruct.Named;

import java.time.Duration;

@Mapper(componentModel = "spring")
public interface DurationMapper {

    @Named("durationToString")
    default String durationToString(Duration duration) {
        if (duration == null) {
            return null;
        }
        Long seconds = toSeconds(duration);
        Long hours = seconds / 3600;
        Long minutes = (seconds % 3600) / 60;
        Long secs = seconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }

    @Named("durationToSeconds")
    default Long toSeconds(Duration duration) {
        return duration != null ? duration.getSeconds() : null;
    }

    @Named("SecondsToDuration")
    default Duration toDuration(Long seconds) {
        return seconds != null ? Duration.ofSeconds(seconds) : null;
    }
}