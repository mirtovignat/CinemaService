package com.example.demo.service.movie;

import com.example.demo.cursor.CursorPageRequest;
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
        String cursor = cursorPageRequest.cursor();
        boolean hasCursor = cursor != null && !cursor.isBlank();
        return movieRepository.fetchDirectorsKeyset(
                cursorPageRequest.query() != null ? cursorPageRequest.query() : "",
                hasCursor,
                hasCursor ? cursor : "",
                PageRequest.of(0, pageSize)
        );
    }

    @Transactional(readOnly = true)
    public Slice<String> searchGenres(CursorPageRequest cursorPageRequest) {
        String cursor = cursorPageRequest.cursor();
        boolean hasCursor = cursor != null && !cursor.isBlank();
        return movieRepository.fetchGenresKeyset(
                cursorPageRequest.query() != null ? cursorPageRequest.query() : "",
                hasCursor,
                hasCursor ? cursor : "",
                PageRequest.of(0, pageSize)
        );
    }

    @Transactional(readOnly = true)
    public MovieRangesSnapshot getRanges() {
        return movieRangesSnapshotMapper.toSnapshot(movieRepository.findRanges());
    }
}