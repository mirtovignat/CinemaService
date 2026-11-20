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
        if (duration == null) {
            return null;
        }
        return duration.getSeconds();
    }

    @Named("secondsToDuration")
    public Duration secondsToDuration(Long seconds) {
        if (seconds == null) {
            return null;
        }
        return Duration.ofSeconds(seconds);
    }
}