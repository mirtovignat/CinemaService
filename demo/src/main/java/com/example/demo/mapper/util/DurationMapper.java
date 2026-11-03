package com.example.demo.mapper.util;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.time.Duration;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Component
public class DurationMapper {

    @Named("durationToString")
    public String durationToString(Duration duration) {
        if (duration == null) {
            return null;
        }
        Long seconds = toSeconds(duration);
        Long hours = seconds / 3600;
        Long minutes = (seconds % 3600) / 60;
        Long secs = seconds % 60;
        return String.format("%d:%02d:%02d", hours, minutes, secs);
    }

    public Long toSeconds(Duration duration) {
        return duration != null ? duration.getSeconds() : null;
    }

    public Duration toDuration(Long seconds) {
        return seconds != null ? Duration.ofSeconds(seconds) : null;
    }
}