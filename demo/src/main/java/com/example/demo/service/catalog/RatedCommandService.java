package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.repository.projection.MovieRatingStatsProjection;
import com.example.demo.service.user.UserQueryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@AllArgsConstructor
@Service
public class RatedCommandService {

    private final UserQueryService userQueryService;
    private final RatedMovieRepository ratedMovieRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public void addOrUpdateRating(Long movieId, Long userId, RatedMovieFormDTO ratedMovieFormDTO) {
        User user = userQueryService.getUser(userId);
        Movie movie = movieRepository.findByIdOrThrow(movieId);

        RatedMovie ratedMovie = ratedMovieRepository
                .findByMovieIdAndUserIdIncludingDeleted(movieId, userId)
                .orElse(null);

        boolean isNew = ratedMovie == null;
        if (isNew) {
            ratedMovie = AbstractCatalogItem.init(RatedMovie.builder().build(), user, movie);
        }

        ratedMovie.setRatingValue(ratedMovieFormDTO.rating());
        ratedMovie.setReview(ratedMovieFormDTO.review());
        ratedMovie.getDeletableState().setDeleted(false);

        ratedMovieRepository.saveAndFlush(ratedMovie);
        recalculateMovieRating(movie);
    }

    @Transactional
    public void deleteRating(Long movieId, Long userId) {
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        RatedMovie ratedMovie = ratedMovieRepository.findByMovieIdAndUserId(movieId, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
        ratedMovieRepository.delete(ratedMovie);
        recalculateMovieRating(movie);
    }

    private void recalculateMovieRating(Movie movie) {
        MovieRatingStatsProjection movieRatingStatsProjection =
                ratedMovieRepository.calculateRatingStats(movie.getId());

        BigDecimal averageRating = movieRatingStatsProjection != null
                ? movieRatingStatsProjection.getAvgRating()
                : null;

        movie.setRating(averageRating != null
                ? averageRating.setScale(2, RoundingMode.HALF_UP)
                : null);

        movieRepository.save(movie);
    }
}