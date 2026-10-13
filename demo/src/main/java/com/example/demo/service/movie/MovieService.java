package com.example.demo.service.movie;

import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.dto.movie.*;
import com.example.demo.filter.MovieFilter;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.service.linked_collection.CartService;
import com.example.demo.service.linked_collection.FavouritesService;
import com.example.demo.service.purchased.PurchasedQueryService;
import com.example.demo.service.rated.RatedQueryService;
import com.example.demo.util.MovieSearchHelper;
import com.example.demo.util.MovieUserStatusHelper;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@AllArgsConstructor
@Service
@Transactional(readOnly = true)
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final PurchasedQueryService purchasedQueryService;
    private final FavouritesService favouritesService;
    private final CartService cartService;
    private final RatedQueryService ratedQueryService;
    private final MovieUserStatusHelper movieUserStatusHelper;
    private final MovieSearchHelper movieSearchHelper;

    @Transactional
    public void save(Movie movie) {
        movieRepository.save(movie);
    }

    public Page<MovieForUser<MovieCardViewDTO>> getMovies(MovieFilter movieFilter,
                                                          String title,
                                                          Pageable pageable,
                                                          Long userId) {
        Page<Movie> moviePage = movieSearchHelper.findMovies(movieFilter, title, pageable);
        return enrichWithStatuses(moviePage, userId);
    }

    public Page<MovieForUser<MovieCardViewDTO>> getMoviesByGenre(String genre,
                                                                 Pageable pageable,
                                                                 Long userId) {
        Page<Movie> moviePage = movieRepository.findAllDistinctByGenre(pageable, genre);
        return enrichWithStatuses(moviePage, userId);
    }

    @Transactional(readOnly = true)
    public MovieForUser<MovieDetailsViewDTO> getCard(Long movieId, Long userId) {
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        MovieDetailsViewDTO movieDetails = movieMapper.toDetails(movie);
        MovieUserStatus movieUserStatus = (userId != null)
                ? calculateUserStatus(movieId, userId)
                : MovieUserStatus.builder().build();
        return MovieForUser.<MovieDetailsViewDTO>builder()
                .movie(movieDetails)
                .status(movieUserStatus)
                .build();
    }

    public Double getMovieRating(Long id) {
        return movieRepository.findRatingById(id)
                .orElse(null);
    }

    public List<MovieSearchDTO> getMovieSearchDTO(String movieSearchQuery) {
        return movieSearchHelper.getSuggestions(movieSearchQuery);
    }

    public Page<MovieForUser<MovieCardViewDTO>> enrichWithStatuses(Page<Movie> moviePage,
                                                                   Long userId) {
        if (moviePage.isEmpty()) {
            return Page.empty(moviePage.getPageable());
        }
        if (userId != null) {
            return movieUserStatusHelper.enrichWithUserStatuses(
                    moviePage, userId, movieMapper::toCard);
        }
        return moviePage.map(movie -> MovieForUser.<MovieCardViewDTO>builder()
                .movie(movieMapper.toCard(movie))
                .status(MovieUserStatus.builder().build())
                .build());
    }

    private MovieUserStatus calculateUserStatus(Long movieId, Long userId) {
        boolean isMovieBought = purchasedQueryService.isMoviePurchasedByUser(movieId, userId);
        boolean isMovieInCart = !isMovieBought && cartService.existsInCollection(movieId, userId);
        boolean isMovieInFavourites = favouritesService.existsInCollection(movieId, userId);
        ReviewViewDTO ownReview = ratedQueryService.getRating(movieId, userId);
        return MovieUserStatus.builder()
                .bought(isMovieBought)
                .inCart(isMovieInCart)
                .inFavourites(isMovieInFavourites)
                .ownRating(ownReview)
                .build();
    }

    public Movie getMovie(Long movieId) {
        return movieRepository.findByIdOrThrow(movieId);
    }
}