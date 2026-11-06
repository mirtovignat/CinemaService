package com.example.demo.service.cursor;

import com.example.demo.config.properties.RecommendationsProperties;
import com.example.demo.cursor.RecommendationCursorKey;
import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.RecommendationKeyCodec;
import com.example.demo.cursor.context.RecommendationCursorContext;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class RecommendationCursorService
        extends AbstractCursorService<Movie, MovieCardViewDTO, RecommendationCursorContext> {

    private final MovieRepository movieRepository;
    private final RecommendationKeyCodec recommendationKeyCodec;

    public RecommendationCursorService(CursorCodec cursorCodec,
                                       RecommendationKeyCodec recommendationKeyCodec,
                                       MovieRepository movieRepository,
                                       MovieMapper movieMapper,
                                       RecommendationsProperties recommendationsProperties) {
        super(cursorCodec,
                recommendationsProperties.cursorPageResponseSize(),
                movie -> recommendationKeyCodec.encode(movie.getRating(), movie.getId()),
                movies -> movies.stream()
                        .map(movie -> movieMapper.toCard(movie, 0L))
                        .toList()
        );

        this.movieRepository = movieRepository;
        this.recommendationKeyCodec = recommendationKeyCodec;
    }

    @Override
    protected Slice<Movie> fetchSlice(String cursorValue,
                                      SortDirection sortDirection,
                                      String searchQuery,
                                      PageRequest pageRequest,
                                      RecommendationCursorContext recommendationCursorContext) {

        boolean hasCursor = cursorValue != null;

        BigDecimal cursorRating = null;
        Long cursorIdentifier = null;
        if (hasCursor) {
            RecommendationCursorKey recommendationCursorKey = recommendationKeyCodec.decode(cursorValue);
            cursorRating = recommendationCursorKey.movieRating();
            cursorIdentifier = recommendationCursorKey.movieId();
        }

        return movieRepository.findRecommendedMoviesSliceKeyset(
                recommendationCursorContext.excludedMovieIds(),
                hasCursor,
                cursorRating,
                cursorIdentifier,
                pageRequest
        );
    }
}