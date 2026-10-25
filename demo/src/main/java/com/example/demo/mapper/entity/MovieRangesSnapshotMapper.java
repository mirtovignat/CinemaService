package com.example.demo.mapper.entity;

import com.example.demo.dto.movie.range.MovieRangesSnapshot;
import com.example.demo.dto.movie.range.Range;
import com.example.demo.mapper.util.DurationMapper;
import com.example.demo.repository.projection.MovieRangeProjection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MovieRangesSnapshotMapper {

    private final DurationMapper durationMapper;

    public MovieRangesSnapshot toSnapshot(MovieRangeProjection projection) {
        if (projection == null) {
            return null;
        }
        return MovieRangesSnapshot.builder()
                .price(Range.of(projection.getMinPrice(), projection.getMaxPrice()))
                .duration(Range.of(
                        durationMapper.toSeconds(projection.getMinDuration()),
                        durationMapper.toSeconds(projection.getMaxDuration())))
                .releaseDate(Range.of(
                        projection.getMinReleaseDate(),
                        projection.getMaxReleaseDate()))
                .build();
    }
}