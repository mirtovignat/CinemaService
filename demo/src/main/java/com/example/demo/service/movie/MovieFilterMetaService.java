package com.example.demo.service.movie;

import com.example.demo.dto.movie.range.MovieRangesSnapshot;
import com.example.demo.dto.request.CursorPageRequest;
import com.example.demo.mapper.util.MovieRangesSnapshotMapper;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.service.movie.keyset.ActorKeysetService;
import com.example.demo.service.movie.keyset.DirectorKeysetService;
import com.example.demo.service.movie.keyset.GenreKeysetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MovieFilterMetaService {

    private final MovieRepository movieRepository;
    private final ActorKeysetService actorKeysetService;
    private final DirectorKeysetService directorKeysetService;
    private final GenreKeysetService genreKeysetService;
    private final MovieRangesSnapshotMapper movieRangesSnapshotMapper;

    @Transactional(readOnly = true)
    public List<String> getAllGenres() {
        return movieRepository.findAllDistinctGenres();
    }

    @Transactional(readOnly = true)
    public Slice<String> searchDirectors(CursorPageRequest cursorPageRequest) {
        return directorKeysetService.search(cursorPageRequest);
    }

    @Transactional(readOnly = true)
    public Slice<String> searchGenres(CursorPageRequest cursorPageRequest) {
        return genreKeysetService.search(cursorPageRequest);
    }

    @Transactional(readOnly = true)
    public Slice<String> searchActors(CursorPageRequest cursorPageRequest) {
        return actorKeysetService.search(cursorPageRequest);
    }

    @Transactional(readOnly = true)
    public MovieRangesSnapshot getRanges() {
        return movieRangesSnapshotMapper.toSnapshot(movieRepository.findRanges());
    }
}