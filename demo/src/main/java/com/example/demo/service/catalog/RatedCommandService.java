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

@AllArgsConstructor
@Service
public class RatedCommandService {

    private final UserQueryService userQueryService;
    private final RatedMovieRepository ratedMovieRepository;
    private final MovieRepository movieRepository;

    @Transactional
    public void addOrUpdateRating(Long movieId, Long userId, RatedMovieFormDTO dto) {
        User user = userQueryService.getUser(userId);
        Movie movie = movieRepository.findByIdOrThrow(movieId);

        RatedMovie ratedMovie = ratedMovieRepository
                .findByMovieIdAndUserIdIncludingDeleted(movieId, userId)
                .orElse(null);

        boolean isNew = ratedMovie == null;
        if (isNew) {
            ratedMovie = AbstractCatalogItem.init(RatedMovie.builder().build(), user, movie);
        }

        ratedMovie.setRatingValue(dto.rating());
        ratedMovie.setReview(dto.review());
        ratedMovie.getDeletableState().setDeleted(false);

        ratedMovieRepository.saveAndFlush(ratedMovie);
    }

    @Transactional
    public void deleteRating(Long movieId, Long userId) {
        movieRepository.findByIdOrThrow(movieId);
        int affected = ratedMovieRepository.softDeleteByMovieIdAndUserId(movieId, userId);
        if (affected == 0) {
            throw BusinessException.of(ErrorCode.ENTITY_NOT_FOUND);
        }
    }
}