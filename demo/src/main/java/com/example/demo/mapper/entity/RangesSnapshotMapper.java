package com.example.demo.mapper.entity;

import com.example.demo.dto.movie.range.Range;
import com.example.demo.dto.movie.range.RangesSnapshot;
import com.example.demo.repository.projection.RangeProjection;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RangesSnapshotMapper {

    public RangesSnapshot toSnapshot(RangeProjection projection) {
        if (projection == null) {
            return null;
        }
        return RangesSnapshot.builder()
                .price(Range.of(projection.getMinPrice(), projection.getMaxPrice()))
                .duration(Range.of(
                        toSeconds(projection.getMinDuration()),
                        toSeconds(projection.getMaxDuration())))
                .releaseDate(Range.of(
                        projection.getMinReleaseDate(),
                        projection.getMaxReleaseDate()))
                .build();
    }

    protected Integer toSeconds(Duration duration) {
        return duration != null ? (int) duration.getSeconds() : null;
    }
}