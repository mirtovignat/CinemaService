package com.example.demo.service.movie;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
public class MovieRecommendationService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final PurchasedMovieRepository purchasedMovieRepository;
    private final RatedMovieRepository ratedMovieRepository;
    private final int pageSize;

    public MovieRecommendationService(MovieRepository movieRepository,
                                      MovieMapper movieMapper,
                                      CartItemRepository cartItemRepository,
                                      FavouriteMovieRepository favouriteMovieRepository,
                                      PurchasedMovieRepository purchasedMovieRepository,
                                      RatedMovieRepository ratedMovieRepository,
                                      @Value("${app.recommendations.cursorPageResponse-size:8}")
                                      int pageSize) {
        this.movieRepository = movieRepository;
        this.movieMapper = movieMapper;
        this.cartItemRepository = cartItemRepository;
        this.favouriteMovieRepository = favouriteMovieRepository;
        this.purchasedMovieRepository = purchasedMovieRepository;
        this.ratedMovieRepository = ratedMovieRepository;
        this.pageSize = pageSize;
    }

    public Slice<MovieCardViewDTO> getRecommendedMoviesSlice(Long userId, int page) {
        Pageable pageable = PageRequest.of(page, pageSize);
        Set<Long> excludedMovieIds = collectExcludedMovieIds(userId);
        List<Long> excludedMovieIdsList = excludedMovieIds.isEmpty() ? null : new ArrayList<>(excludedMovieIds);
        Slice<Movie> recommendedMoviesSlice = movieRepository.findRecommendedMoviesSlice(excludedMovieIdsList, pageable);
        return recommendedMoviesSlice.map(movieMapper::toCard);
    }

    public Slice<MovieCardViewDTO> getFirstRecommendationsPage(Long userId) {
        return getRecommendedMoviesSlice(userId, 0);
    }

    @Transactional(readOnly = true)
    public Set<Long> collectExcludedMovieIds(Long userId) {
        Set<Long> excludedMovieIds = new LinkedHashSet<>();
        excludedMovieIds.addAll(cartItemRepository.findMovieIdsByUserId(userId));
        excludedMovieIds.addAll(favouriteMovieRepository.findMovieIdsByUserId(userId));
        excludedMovieIds.addAll(purchasedMovieRepository.findMovieIdsByUserId(userId));
        excludedMovieIds.addAll(ratedMovieRepository.findMovieIdsByUserId(userId));
        return excludedMovieIds;
    }
}