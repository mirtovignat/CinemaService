package com.example.demo.service.movie;

import com.example.demo.dto.movie.range.MovieRangesSnapshot;
import com.example.demo.dto.request.CursorPageRequest;
import com.example.demo.mapper.util.MovieRangesSnapshotMapper;
import com.example.demo.repository.entity.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
@Service
public class MovieFilterMetaService {

    private final MovieRepository movieRepository;
    private final MovieRangesSnapshotMapper movieRangesSnapshotMapper;
    private final StringKeysetPaginator stringKeysetPaginator;
    private final int pageSize;

    public MovieFilterMetaService(MovieRepository movieRepository,
                                  MovieRangesSnapshotMapper movieRangesSnapshotMapper,
                                  StringKeysetPaginator stringKeysetPaginator,
                                  @Value("${app.filters.page-size:50}") int pageSize) {
        this.movieRepository = movieRepository;
        this.movieRangesSnapshotMapper = movieRangesSnapshotMapper;
        this.stringKeysetPaginator = stringKeysetPaginator;
        this.pageSize = pageSize;
    }

    @Transactional(readOnly = true)
    public List<String> getAllGenres() {
        return movieRepository.findAllDistinctGenres();
    }

    @Transactional(readOnly = true)
    public Slice<String> searchDirectors(CursorPageRequest cursorPageRequest) {
        return stringKeysetPaginator.fetchPage(cursorPageRequest, pageSize, movieRepository::fetchDirectorsKeyset);
    }

    @Transactional(readOnly = true)
    public Slice<String> searchGenres(CursorPageRequest cursorPageRequest) {
        return stringKeysetPaginator.fetchPage(cursorPageRequest, pageSize, movieRepository::fetchGenresKeyset);
    }

    @Transactional(readOnly = true)
    public MovieRangesSnapshot getRanges() {
        return movieRangesSnapshotMapper.toSnapshot(movieRepository.findRanges());
    }
}