package com.example.demo.service.movie;

import com.example.demo.config.properties.FiltersProperties;
import com.example.demo.dto.movie.range.MovieRangesSnapshot;
import com.example.demo.dto.request.CursorPageRequest;
import com.example.demo.mapper.util.MovieRangesSnapshotMapper;
import com.example.demo.repository.entity.ActorRepository;
import com.example.demo.repository.entity.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieFilterMetaService {

    private final MovieRepository movieRepository;
    private final ActorRepository actorRepository;
    private final StringKeysetPaginator stringKeysetPaginator;
    private final FiltersProperties filtersProperties;
    private final MovieRangesSnapshotMapper movieRangesSnapshotMapper;

    @Transactional(readOnly = true)
    public List<String> getAllGenres() {
        return movieRepository.findAllDistinctGenres();
    }

    @Transactional(readOnly = true)
    public Slice<String> searchDirectors(CursorPageRequest cursorPageRequest) {
        return stringKeysetPaginator.fetchPage(
                cursorPageRequest, filtersProperties.pageSize(), movieRepository::fetchDirectorsKeyset);
    }

    @Transactional(readOnly = true)
    public Slice<String> searchGenres(CursorPageRequest cursorPageRequest) {
        return stringKeysetPaginator.fetchPage(
                cursorPageRequest, filtersProperties.pageSize(), movieRepository::fetchGenresKeyset);
    }

    @Transactional(readOnly = true)
    public Slice<String> searchActors(CursorPageRequest cursorPageRequest) {
        return stringKeysetPaginator.fetchPage(
                cursorPageRequest, filtersProperties.pageSize(), actorRepository::fetchActorsKeyset);
    }

    @Transactional(readOnly = true)
    public MovieRangesSnapshot getRanges() {
        return movieRangesSnapshotMapper.toSnapshot(movieRepository.findRanges());
    }
}