package com.example.demo.mapper.util;

import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class DurationMapper {

    @Named("durationToString")
    public String durationToString(Duration duration) {
        if (duration == null) {
            return null;
        }
        long seconds = duration.getSeconds();
        long hours = seconds / 3600;
        long minutes = (seconds % 3600) / 60;
        long secs = seconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }

    @Named("durationToSeconds")
    public Long durationToSeconds(Duration duration) {
        return duration == null ? null : duration.getSeconds();
    }

    @Named("secondsToDuration")
    public Duration secondsToDuration(Long seconds) {
        return seconds == null ? null : Duration.ofSeconds(seconds);
    }
}