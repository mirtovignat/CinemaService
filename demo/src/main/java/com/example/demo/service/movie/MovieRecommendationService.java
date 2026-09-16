package com.example.demo.service.movie;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.repository.entity.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MovieRecommendationService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final PurchasedMovieRepository purchasedMovieRepository;
    private final RatedMovieRepository ratedMovieRepository;

    public Slice<MovieCardViewDTO> getRecommendedMoviesSlice(Long userId, Pageable pageable) {
        Set<Long> excludedIds = collectExcludedMovieIds(userId);
        List<Long> idsParam = excludedIds.isEmpty() ? null : new ArrayList<>(excludedIds);
        return movieRepository.findRecommendedMoviesSlice(idsParam, pageable)
                .map(movieMapper::toCard);
    }

    private Set<Long> collectExcludedMovieIds(Long userId) {
        Set<Long> excludedIds = new LinkedHashSet<>();
        excludedIds.addAll(cartItemRepository.findMovieIdsByUserId(userId));
        excludedIds.addAll(favouriteMovieRepository.findMovieIdsByUserId(userId));
        excludedIds.addAll(purchasedMovieRepository.findMovieIdsByUserId(userId));
        excludedIds.addAll(ratedMovieRepository.findMovieIdsByUserId(userId));
        return excludedIds;
    }
}