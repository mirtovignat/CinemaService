package com.example.demo.service.cursor;

import com.example.demo.cursor.RecommendationCursorKey;
import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.codec.CursorCodec;
import com.example.demo.cursor.codec.RecommendationKeyCodec;
import com.example.demo.cursor.context.RecommendationCursorContext;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.util.normalizer.TitleNormalizer;
import org.springframework.beans.factory.annotation.Value;
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
                                       TitleNormalizer titleNormalizer,
                                       RecommendationKeyCodec recommendationKeyCodec,
                                       MovieRepository movieRepository,
                                       MovieMapper movieMapper,
                                       @Value("${app.recommendations.cursorPageResponse-size:8}") int pageSize) {
        super(
                cursorCodec,
                titleNormalizer,
                pageSize,
                movie -> recommendationKeyCodec.encode(movie.getRating(), movie.getId()),
                movies -> movies.stream()
                        .map(movieMapper::toCard)
                        .toList()
        );

        this.movieRepository = movieRepository;
        this.recommendationKeyCodec = recommendationKeyCodec;
    }

    @Override
    protected Slice<Movie> fetchSlice(String cursorValue,
                                      SortDirection sortDirection,
                                      String query,
                                      PageRequest pageRequest,
                                      RecommendationCursorContext recommendationCursorContext) {
        if (sortDirection == SortDirection.ASC) {
            throw new IllegalArgumentException("Recommendations support only DESC");
        }

        boolean hasCursor = cursorValue != null;

        BigDecimal cursorRating = null;
        Long cursorIdentifier = null;
        if (hasCursor) {
            RecommendationCursorKey recommendationCursorKey = recommendationKeyCodec.decode(cursorValue);
            cursorRating = recommendationCursorKey.rating();
            cursorIdentifier = recommendationCursorKey.identifier();
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