package com.example.demo.mapper.util;

import com.example.demo.dto.movie.range.MovieRangesSnapshot;
import com.example.demo.dto.movie.range.Range;
import com.example.demo.repository.projection.MovieRangeProjection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@AllArgsConstructor
@Component
public class MovieRangesSnapshotMapper {

    private final DurationMapper durationMapper;

    public MovieRangesSnapshot toSnapshot(MovieRangeProjection movieRangeProjection) {
        if (movieRangeProjection == null) {
            return null;
        }
        return MovieRangesSnapshot.builder()
                .price(Range.of(movieRangeProjection.getMinPrice(), movieRangeProjection.getMaxPrice()))
                .duration(Range.of(
                        durationMapper.durationToSeconds(movieRangeProjection.getMinDuration()),
                        durationMapper.durationToSeconds(movieRangeProjection.getMaxDuration())))
                .releaseDate(Range.of(
                        movieRangeProjection.getMinReleaseDate(),
                        movieRangeProjection.getMaxReleaseDate()))
                .build();
    }
}