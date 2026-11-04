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
import com.example.demo.service.user.UserQueryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@AllArgsConstructor
@Service
public class RatedCommandService {

    private final UserQueryService userQueryService;
    private final RatedMovieRepository ratedMovieRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public RatingAction addOrUpdateRating(Long movieId, Long userId, RatedMovieFormDTO ratedMovieFormDTO) {
        User user = userQueryService.getUser(userId);
        Movie movie = movieRepository.findByIdOrThrow(movieId);

        RatedMovie ratedMovie = ratedMovieRepository
                .findByMovieIdAndUserIdForUpdate(movieId, userId)
                .orElse(null);

        BigDecimal newRating = ratedMovieFormDTO.rating();

        if (ratedMovie == null) {
            ratedMovie = AbstractCatalogItem.init(RatedMovie.builder().build(), user, movie);
            ratedMovie.setRatingValue(newRating);
            ratedMovie.setReview(ratedMovieFormDTO.review());

            ratedMovieRepository.saveAndFlush(ratedMovie);
            movieRepository.applyRatingDelta(movieId, newRating, 1L);
            return RatingAction.CREATED;
        }

        BigDecimal delta = newRating.subtract(ratedMovie.getRatingValue());
        ratedMovie.setRatingValue(newRating);
        ratedMovie.setReview(ratedMovieFormDTO.review());

        ratedMovieRepository.saveAndFlush(ratedMovie);
        if (delta.signum() != 0) {
            movieRepository.applyRatingDelta(movieId, delta, 0L);
        }
        return RatingAction.UPDATED;
    }

    @Transactional
    public void deleteRating(Long movieId, Long userId) {
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        RatedMovie ratedMovie = ratedMovieRepository
                .findByMovieIdAndUserIdForUpdate(movieId, userId)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));

        BigDecimal oldRating = ratedMovie.getRatingValue();

        ratedMovieRepository.delete(ratedMovie);
        movieRepository.applyRatingDelta(movieId, oldRating.negate(), -1L);
    }
}