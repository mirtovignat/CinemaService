package com.example.demo.service.movie;

import com.example.demo.dto.movie.*;
import com.example.demo.filter.MovieFilter;
import com.example.demo.filter.MovieSearchHelper;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final MovieUserStatusBatchLoader movieUserStatusBatchLoader;
    private final MovieSearchHelper movieSearchHelper;
    private final MovieReviewCountService movieReviewCountService;
    private final int suggestionsLimit;

    public MovieService(MovieRepository movieRepository,
                        MovieMapper movieMapper,
                        MovieUserStatusBatchLoader movieUserStatusBatchLoader,
                        MovieSearchHelper movieSearchHelper,
                        MovieReviewCountService movieReviewCountService,
                        @Value("${app.suggestions.catalog-limit:10}") int suggestionsLimit) {
        this.movieRepository = movieRepository;
        this.movieMapper = movieMapper;
        this.movieUserStatusBatchLoader = movieUserStatusBatchLoader;
        this.movieSearchHelper = movieSearchHelper;
        this.movieReviewCountService = movieReviewCountService;
        this.suggestionsLimit = suggestionsLimit;
    }

    @Transactional(readOnly = true)
    public Page<MovieForUser<MovieCardViewDTO>> getMovies(MovieFilter movieFilter,
                                                          String title,
                                                          Pageable pageable,
                                                          Long userId) {
        Page<Movie> moviePage = movieSearchHelper.findMovies(movieFilter, title, pageable);
        return enrichWithStatuses(moviePage, userId);
    }

    @Transactional(readOnly = true)
    public Page<MovieForUser<MovieCardViewDTO>> getMoviesByGenre(String genre,
                                                                 Pageable pageable,
                                                                 Long userId) {
        Page<Movie> moviePage = movieRepository.findByGenre(genre, pageable);
        return enrichWithStatuses(moviePage, userId);
    }

    @Transactional(readOnly = true)
    public MovieForUser<MovieDetailsViewDTO> getCard(Long movieId, Long userId) {
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        long ratingsCount = movieReviewCountService.countForMovie(movieId);

        MovieDetailsViewDTO movieDetailsViewDTO = movieMapper.toDetails(movie, ratingsCount);

        MovieUserStatus movieUserStatus;
        if (userId != null) {
            movieUserStatus = movieUserStatusBatchLoader.loadForMovies(List.of(movieId), userId)
                    .getOrDefault(movieId, MovieUserStatus.EMPTY);
        } else {
            movieUserStatus = MovieUserStatus.EMPTY;
        }

        return MovieForUser.<MovieDetailsViewDTO>builder()
                .movie(movieDetailsViewDTO)
                .movieUserStatus(movieUserStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public BigDecimal getMovieRating(Long movieId) {
        return movieRepository.findRatingById(movieId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MovieSearchDTO> getMovieSearchDTO(String movieSearchQuery) {
        return movieSearchHelper.getSuggestions(movieSearchQuery, suggestionsLimit);
    }

    @Transactional(readOnly = true)
    public Page<MovieForUser<MovieCardViewDTO>> enrichWithStatuses(Page<Movie> moviePage, Long userId) {
        if (moviePage.isEmpty()) {
            return Page.empty(moviePage.getPageable());
        }

        List<Long> movieIds = moviePage.getContent().stream().map(Movie::getId).toList();
        Map<Long, Long> reviewCountsByMovieId = movieReviewCountService.countsByMovieIds(movieIds);

        Map<Long, MovieUserStatus> movieUserStatusesByMovieId;
        if (userId != null) {
            movieUserStatusesByMovieId = movieUserStatusBatchLoader.loadForMovies(movieIds, userId);
        } else {
            movieUserStatusesByMovieId = Map.of();
        }

        return moviePage.map(movie -> MovieForUser.<MovieCardViewDTO>builder()
                .movie(movieMapper.toCard(movie, reviewCountsByMovieId.getOrDefault(movie.getId(), 0L)))
                .movieUserStatus(movieUserStatusesByMovieId.getOrDefault(movie.getId(), MovieUserStatus.EMPTY))
                .build());
    }

    public Movie getMovie(Long movieId) {
        return movieRepository.findByIdOrThrow(movieId);
    }

    @Transactional(readOnly = true)
    public Set<Long> collectExcludedMovieIds(Long userId) {
        if (userId == null) {
            return new LinkedHashSet<>();
        }
        return new LinkedHashSet<>(movieRepository.findExcludedMovieIdsByUserId(userId));
    }
}