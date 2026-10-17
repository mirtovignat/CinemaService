package com.example.demo.service.catalog.cursor;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.cursor.CursorCodec;
import com.example.demo.service.cursor.KeyUtils;
import com.example.demo.service.cursor.context.UserCursorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class RatedCursorService
        extends AbstractCursorService<RatedMovie, RatedMovieViewDTO, UserCursorContext> {

    private final RatedMovieRepository ratedMovieRepository;

    public RatedCursorService(CursorCodec cursorCodec,
                              RatedMovieRepository ratedMovieRepository,
                              RatedMapper ratedMapper,
                              @Value("${app.catalog.page-size:20}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
                ratedMovie -> KeyUtils.encodeTimestampAndIdentifier(
                        ratedMovie.getAddedAt(), ratedMovie.getAbstractCatalogItemId().getMovieId()),
                ratedMovies -> ratedMovies.stream()
                        .map(ratedMapper::toOwnerView)
                        .toList());
        this.ratedMovieRepository = ratedMovieRepository;
    }

    @Override
    protected Slice<RatedMovie> fetchSlice(String cursorValue,
                                           String sortDirection,
                                           PageRequest pageRequest,
                                           UserCursorContext context) {
        Long userId = context.userId();
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorAddedAt = hasCursor ? KeyUtils.decodeTimestamp(cursorValue) : null;
        Long cursorMovieId = hasCursor ? KeyUtils.decodeIdentifier(cursorValue) : null;
        return "asc".equals(sortDirection)
                ? ratedMovieRepository.fetchRatedAscending(userId, hasCursor, cursorAddedAt, cursorMovieId, pageRequest)
                : ratedMovieRepository.fetchRatedDescending(userId, hasCursor, cursorAddedAt, cursorMovieId, pageRequest);
    }
}