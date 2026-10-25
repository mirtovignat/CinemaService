package com.example.demo.service.movie;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.CursorUtils;
import com.example.demo.dto.movie.range.MovieRangesSnapshot;
import com.example.demo.mapper.entity.MovieRangesSnapshotMapper;
import com.example.demo.repository.entity.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovieFilterMetaService {

    private final MovieRepository movieRepository;
    private final MovieRangesSnapshotMapper movieRangesSnapshotMapper;
    private final int pageSize;

    public MovieFilterMetaService(MovieRepository movieRepository,
                                  MovieRangesSnapshotMapper movieRangesSnapshotMapper,
                                  @Value("${app.filters.page-size:50}") int pageSize) {
        this.movieRepository = movieRepository;
        this.movieRangesSnapshotMapper = movieRangesSnapshotMapper;
        this.pageSize = pageSize;
    }

    @Transactional(readOnly = true)
    public List<String> getAllGenres() {
        return movieRepository.findAllDistinctGenres();
    }

    @Transactional(readOnly = true)
    public Slice<String> searchDirectors(CursorPageRequest cursorPageRequest) {
        List<String> rows = movieRepository.fetchDirectorsKeyset(
                CursorUtils.safeQuery(cursorPageRequest),
                CursorUtils.hasCursor(cursorPageRequest),
                CursorUtils.safeCursor(cursorPageRequest),
                pageSize + 1
        );

        return CursorUtils.buildLimitedSlice(rows, pageSize);
    }

    @Transactional(readOnly = true)
    public Slice<String> searchGenres(CursorPageRequest cursorPageRequest) {
        Slice<String> genresSlice = movieRepository.fetchGenresKeyset(
                CursorUtils.safeQuery(cursorPageRequest),
                CursorUtils.hasCursor(cursorPageRequest),
                CursorUtils.safeCursor(cursorPageRequest),
                PageRequest.of(0, pageSize + 1)
        );

        return CursorUtils.buildLimitedSlice(genresSlice.getContent(), pageSize);
    }

    @Transactional(readOnly = true)
    public MovieRangesSnapshot getRanges() {
        return movieRangesSnapshotMapper.toSnapshot(movieRepository.findRanges());
    }
}