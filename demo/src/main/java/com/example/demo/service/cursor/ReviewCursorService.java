package com.example.demo.service.cursor;

import com.example.demo.config.properties.ReviewsProperties;
import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.IdCodec;
import com.example.demo.cursor.context.ReviewCursorContext;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.catalog.ReviewViewAssembler;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

@Service
public class ReviewCursorService
        extends AbstractCursorService<RatedMovie, ReviewViewDTO, ReviewCursorContext> {

    private final RatedMovieRepository ratedMovieRepository;
    private final IdCodec idCodec;

    public ReviewCursorService(CursorCodec cursorCodec,
                               IdCodec idCodec,
                               RatedMovieRepository ratedMovieRepository,
                               ReviewViewAssembler reviewViewAssembler,
                               ReviewsProperties reviewsProperties) {
        super(cursorCodec,
                reviewsProperties.cursorPageResponseSize(),
                ratedMovie -> idCodec.encode(ratedMovie.getId()),
                ratedMovies -> ratedMovies.stream()
                        .map(ratedMovie -> reviewViewAssembler.assemble(ratedMovie, null))
                        .toList());
        this.ratedMovieRepository = ratedMovieRepository;
        this.idCodec = idCodec;
    }

    @Override
    protected Slice<RatedMovie> fetchSlice(String cursorValue,
                                           SortDirection sortDirection,
                                           String searchQuery,
                                           PageRequest pageRequest,
                                           ReviewCursorContext reviewCursorContext) {
        Long movieId = reviewCursorContext.movieId();
        Long viewerId = reviewCursorContext.viewerId();
        boolean hasCursor = cursorValue != null;
        Long cursorId;
        if (hasCursor) {
            cursorId = idCodec.decode(cursorValue);
        } else {
            cursorId = null;
        }

        return switch (sortDirection) {
            case ASC -> ratedMovieRepository.fetchReviewsAscending(
                    movieId, viewerId, hasCursor, cursorId, pageRequest);
            case DESC -> ratedMovieRepository.fetchReviewsDescending(
                    movieId, viewerId, hasCursor, cursorId, pageRequest);
        };
    }
}