package com.example.demo.service.movie;

import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.dto.movie.*;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.filter.MovieFilter;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.Genre;
import com.example.demo.model.entity.CartItem;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
import com.example.demo.service.purchased.PurchasedQueryService;
import com.example.demo.util.MovieSearchHelper;
import com.example.demo.util.MovieUserStatusHelper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final PurchasedQueryService purchasedQueryService;
    private final AbstractLinkedCollectionService<FavouriteMovie, FavouriteMovieViewDTO> favouritesService;
    private final AbstractLinkedCollectionService<CartItem, CartItemViewDTO> cartService;
    private final MovieUserStatusHelper userStatusHelper;
    private final MovieSearchHelper movieSearchHelper;

    public Page<MovieForUser<MovieCardViewDTO>> getMovies(MovieFilter movieFilter,
                                                          Pageable pageable,
                                                          Long userId) {
        Page<Movie> moviePage = movieSearchHelper.findMoviesByMovieFilter(
                movieFilter, pageable);
        return enrichWithStatuses(moviePage, userId);
    }

    public Page<MovieForUser<MovieCardViewDTO>> getMoviesByGenre(String genreDisplayName,
                                                                 Pageable pageable,
                                                                 Long userId) {
        Genre genre = Genre.fromDisplayName(genreDisplayName);
        Page<Movie> moviePage = movieRepository.findAllDistinctByGenre(pageable, genre);
        return enrichWithStatuses(moviePage, userId);
    }

    @Transactional
    public MovieForUser<MovieCardDetailsViewDTO> getCard(Long movieId, Long userId) {
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        MovieCardDetailsViewDTO movieCardDetailsViewDTO = movieMapper.toDetails(movie);
        MovieUserStatus status = (userId != null)
                ? calculateUserStatus(movieId, userId)
                : MovieUserStatus.builder().build();
        return new MovieForUser<>(movieCardDetailsViewDTO, status);
    }

    public Double getMovieRating(Long id) {
        return movieRepository.findRatingById(id)
                .orElseThrow(() -> BusinessException.of(ErrorCode.ENTITY_NOT_FOUND));
    }

    public List<MovieSearchDTO> getMovieSearchDTO(String movieSearchQuery) {
        return movieSearchHelper.getSuggestions(movieSearchQuery);
    }

    public List<String> getAllDirectors() {
        return movieRepository.findAllDistinctDirectors();
    }

    public List<String> getAllGenres() {
        return movieRepository.findAllGenresFromEnum()
                .stream()
                .map(genre -> {
                    try {
                        return Genre.valueOf(genre).getDisplayName();
                    } catch (IllegalArgumentException illegalArgumentException) {
                        return genre;
                    }
                })
                .collect(Collectors.toList());
    }

    public Page<MovieForUser<MovieCardViewDTO>> enrichWithStatuses(
            Page<Movie> moviePage, Long userId) {
        if (moviePage.isEmpty()) {
            return Page.empty(moviePage.getPageable());
        }

        if (userId != null) {
            return userStatusHelper.enrichWithUserStatuses(moviePage, userId,
                    movieMapper::toCard);
        }

        return moviePage.map(movie -> new MovieForUser<>(movieMapper.toCard(movie),
                MovieUserStatus.builder().build()));
    }

    private MovieUserStatus calculateUserStatus(Long movieId, Long userId) {
        boolean isBought = purchasedQueryService.isMoviePurchasedByUser(movieId, userId);
        boolean isInCart = !isBought && cartService.existsInCollection(movieId, userId);
        boolean isInFavourites = favouritesService.existsInCollection(movieId, userId);
        return new MovieUserStatus(isBought, isInCart, isInFavourites);
    }

    public Movie getMovie(Long movieId) {
        return movieRepository.findByIdOrThrow(movieId);
    }

    @Transactional
    public void save(Movie movie) {
        movieRepository.save(movie);
    }
}