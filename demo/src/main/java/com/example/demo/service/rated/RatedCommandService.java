package com.example.demo.service.rated;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.model.base.AbstractCatalogItem;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.model.entity.User;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.user.UserQueryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@AllArgsConstructor
@Service
public class RatedCommandService {

    private final MovieService movieService;
    private final UserQueryService userQueryService;
    private final RatedMovieRepository ratedMovieRepository;

    @Transactional
    public void addOrUpdateRating(Long movieId, Long userId,
                                  RatedMovieFormDTO ratedMovieFormDTO) {
        Movie movie = movieService.getMovie(movieId);
        User user = userQueryService.getUser(userId);

        RatedMovie ratedMovie = ratedMovieRepository
                .findByMovieIdAndUserIdIncludingDeleted(movieId, userId)
                .orElse(null);

        boolean isNew = ratedMovie == null;
        if (isNew) {
            ratedMovie = AbstractCatalogItem.init(RatedMovie.builder().build(), user, movie);
        }

        ratedMovie.setRatingValue(ratedMovieFormDTO.rating());
        ratedMovie.setReview(ratedMovieFormDTO.review());
        if (isNew) {
            ratedMovie.setAddedAt(LocalDateTime.now(ZoneOffset.UTC));
        }
        ratedMovie.getDeletableState().setDeleted(false);
        ratedMovieRepository.saveAndFlush(ratedMovie);

        synchronizeMovieRating(movie, movieId);
    }

    @Transactional
    public void deleteRating(Long movieId, Long userId) {
        Movie movie = movieService.getMovie(movieId);

        int affected = ratedMovieRepository.softDeleteByMovieIdAndUserId(movieId, userId);
        if (affected == 0) {
            throw BusinessException.of(ErrorCode.ENTITY_NOT_FOUND);
        }

        synchronizeMovieRating(movie, movieId);
    }

    private void synchronizeMovieRating(Movie movie, Long movieId) {
        BigDecimal average = ratedMovieRepository.calculateAverageRating(movie);
        Long actualCount = ratedMovieRepository.countByMovieId(movieId);
        movie.setRating(average != null ? average.setScale(1, RoundingMode.HALF_UP) : null);
        movie.setRatingsCount(actualCount == null ? 0 : actualCount.intValue());
        movieService.save(movie);
    }
}