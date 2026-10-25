package com.example.demo.service.cursor;

import com.example.demo.cursor.Part;
import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.KeyCodec;
import com.example.demo.cursor.context.ReviewCursorContext;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.util.normalizer.TitleNormalizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class ReviewCursorService
        extends AbstractCursorService<RatedMovie, ReviewViewDTO, ReviewCursorContext> {

    private final RatedMovieRepository ratedMovieRepository;
    private final KeyCodec keyCodec;

    public ReviewCursorService(CursorCodec cursorCodec,
                               KeyCodec keyCodec,
                               TitleNormalizer titleNormalizer,
                               RatedMovieRepository ratedMovieRepository,
                               RatedMapper ratedMapper,
                               @Value("${app.reviews.cursorPageResponse-size:10}") int defaultPageSize) {
        super(cursorCodec,
                titleNormalizer,
                defaultPageSize,
                ratedMovie -> keyCodec.encode(
                        ratedMovie.getAddedAt(),
                        ratedMovie.getAbstractCatalogItemId().getUserId()),
                ratedMovies -> ratedMovies.stream()
                        .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(
                                        ratedMovie,
                                        null
                                )
                        )
                        .toList());
        this.ratedMovieRepository = ratedMovieRepository;
        this.keyCodec = keyCodec;
    }

    @Override
    protected Slice<RatedMovie> fetchSlice(String cursorValue,
                                           SortDirection sortDirection,
                                           String query,
                                           PageRequest pageRequest,
                                           ReviewCursorContext reviewCursorContext) {
        Long movieId = reviewCursorContext.movieId();
        Long viewerId = reviewCursorContext.viewerId();
        boolean hasCursor = cursorValue != null;
        LocalDateTime cursorAddedAt = hasCursor
                ? keyCodec.decode(cursorValue, LocalDateTime::parse, Part.LEFT)
                : null;
        Long cursorUserId = hasCursor
                ? keyCodec.decode(cursorValue, Long::parseLong, Part.RIGHT)
                : null;

        return switch (sortDirection) {
            case ASC -> ratedMovieRepository.fetchReviewsAscending(
                    movieId,
                    viewerId,
                    hasCursor,
                    cursorAddedAt,
                    cursorUserId,
                    pageRequest
            );
            case DESC -> ratedMovieRepository.fetchReviewsDescending(
                    movieId,
                    viewerId,
                    hasCursor,
                    cursorAddedAt,
                    cursorUserId,
                    pageRequest
            );
        };
    }
}