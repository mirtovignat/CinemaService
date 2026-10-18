package com.example.demo.service.cursor;

import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.cursor.util.CursorCodec;
import com.example.demo.service.cursor.util.KeyUtils;
import com.example.demo.service.cursor.util.ReviewCursorContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReviewCursorService
        extends AbstractCursorService<RatedMovie, ReviewViewDTO, ReviewCursorContext> {

    private final RatedMovieRepository ratedMovieRepository;

    public ReviewCursorService(CursorCodec cursorCodec,
                               RatedMovieRepository ratedMovieRepository,
                               RatedMapper ratedMapper,
                               @Value("${app.reviews.page-size:10}") int defaultPageSize) {
        super(cursorCodec,
                defaultPageSize,
                ratedMovie -> KeyUtils.encodeTimestampAndIdentifier(
                        ratedMovie.getAddedAt(), ratedMovie.getAbstractCatalogItemId().getUserId()),
                ratedMovies -> ratedMovies.stream()
                        .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(ratedMovie, null))
                        .toList());
        this.ratedMovieRepository = ratedMovieRepository;
    }

    @Override
    protected Slice<RatedMovie> fetchSlice(String cursorValue,
                                           String sortDirection,
                                           PageRequest pageRequest,
                                           ReviewCursorContext context) {
        Long movieId = context.movieId();
        Long viewerId = context.viewerId();
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorAddedAt = hasCursor ? KeyUtils.decodeTimestamp(cursorValue) : null;
        Long cursorUserId = hasCursor ? KeyUtils.decodeIdentifier(cursorValue) : null;
        return "asc".equals(sortDirection)
                ? ratedMovieRepository.fetchReviewsAscending(
                movieId, viewerId, hasCursor, cursorAddedAt, cursorUserId, pageRequest)
                : ratedMovieRepository.fetchReviewsDescending(
                movieId, viewerId, hasCursor, cursorAddedAt, cursorUserId, pageRequest);
    }
}