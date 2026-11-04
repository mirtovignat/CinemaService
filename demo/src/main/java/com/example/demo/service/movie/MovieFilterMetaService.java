package com.example.demo.service.movie;

import com.example.demo.cursor.CursorPageRequest;
import com.example.demo.cursor.CursorUtils;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.dto.movie.range.MovieRangesSnapshot;
import com.example.demo.mapper.util.MovieRangesSnapshotMapper;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.util.text.LikeEscaper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MovieFilterMetaService {

    private final MovieRepository movieRepository;
    private final MovieRangesSnapshotMapper movieRangesSnapshotMapper;
    private final CursorCodec cursorCodec;
    private final int pageSize;

    public MovieFilterMetaService(MovieRepository movieRepository,
                                  MovieRangesSnapshotMapper movieRangesSnapshotMapper,
                                  CursorCodec cursorCodec,
                                  @Value("${app.filters.page-size:50}") int pageSize) {
        this.movieRepository = movieRepository;
        this.movieRangesSnapshotMapper = movieRangesSnapshotMapper;
        this.cursorCodec = cursorCodec;
        this.pageSize = pageSize;
    }

    @Transactional(readOnly = true)
    public List<String> getAllGenres() {
        return movieRepository.findAllDistinctGenres();
    }

    @Transactional(readOnly = true)
    public Slice<String> searchDirectors(CursorPageRequest cursorPageRequest) {
        String prefix = CursorUtils.normalizeQuery(cursorPageRequest.query());
        boolean hasCursor = CursorUtils.hasCursor(cursorPageRequest);
        String cursor = hasCursor
                ? LikeEscaper.escape(cursorCodec.decode(cursorPageRequest.cursor()).value())
                : "";

        List<String> rows = movieRepository.fetchDirectorsKeyset(prefix, hasCursor, cursor, pageSize + 1);
        return CursorUtils.buildLimitedSlice(rows, pageSize);
    }

    @Transactional(readOnly = true)
    public Slice<String> searchGenres(CursorPageRequest cursorPageRequest) {
        String prefix = CursorUtils.normalizeQuery(cursorPageRequest.query());
        boolean hasCursor = CursorUtils.hasCursor(cursorPageRequest);
        String cursor = hasCursor
                ? LikeEscaper.escape(cursorCodec.decode(cursorPageRequest.cursor()).value())
                : "";

        List<String> rows = movieRepository.fetchGenresKeyset(prefix, hasCursor, cursor, pageSize + 1);
        return CursorUtils.buildLimitedSlice(rows, pageSize);
    }

    @Transactional(readOnly = true)
    public MovieRangesSnapshot getRanges() {
        return movieRangesSnapshotMapper.toSnapshot(movieRepository.findRanges());
    }
}