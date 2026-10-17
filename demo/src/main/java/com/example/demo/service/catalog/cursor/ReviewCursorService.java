package com.example.demo.service.catalog.cursor;

import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.cursor.AbstractCursorService;
import com.example.demo.service.cursor.CursorCodec;
import com.example.demo.service.cursor.KeyUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReviewCursorService
        extends AbstractCursorService<RatedMovie, ReviewViewDTO> {

    private final RatedMovieRepository ratedMovieRepository;

    public ReviewCursorService(CursorCodec cursorCodec,
                               RatedMovieRepository ratedMovieRepository,
                               RatedMapper ratedMapper,
                               @Value("${app.reviews.page-size:10}") int defaultPageSize,
                               @Value("${app.reviews.max-page-size:50}") int maximumPageSize) {
        super(cursorCodec,
                defaultPageSize,
                maximumPageSize,
                ratedMovie -> KeyUtils.encodeTimestampAndIdentifier(
                        ratedMovie.getAddedAt(), ratedMovie.getId().getUserId()),
                ratedMovies -> ratedMovies.stream()
                        .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(ratedMovie, null))
                        .toList());
        this.ratedMovieRepository = ratedMovieRepository;
    }

    @Override
    protected Slice<RatedMovie> fetchSlice(String cursorValue,
                                           String sortDirection,
                                           PageRequest pageRequest,
                                           Long... contextValues) {
        Long movieId = contextValues[0];
        Long viewerId = contextValues.length > 1 && contextValues[1] != null
                ? contextValues[1]
                : -1L;
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