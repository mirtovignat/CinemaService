package com.example.demo.filter;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.service.movie.MovieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
public class MovieFilterFacade {

    private final MovieService movieService;
    private final MovieSortResolver movieSortResolver;
    private final MovieFilterValidator movieFilterValidator;
    private final int defaultPageSize;
    private final int genreDefaultPageSize;

    public MovieFilterFacade(MovieService movieService,
                             MovieSortResolver movieSortResolver,
                             MovieFilterValidator movieFilterValidator,
                             @Value("${app.pagination.default-size:35}") int defaultPageSize,
                             @Value("${app.pagination.genre-default-size:15}") int genreDefaultPageSize) {
        this.movieService = movieService;
        this.movieSortResolver = movieSortResolver;
        this.movieFilterValidator = movieFilterValidator;
        this.defaultPageSize = defaultPageSize;
        this.genreDefaultPageSize = genreDefaultPageSize;
    }

    public Page<MovieForUser<MovieCardViewDTO>> filterMovies(
            MovieFilter movieFilter,
            String title,
            PageRequestContext pageRequestContext,
            Long userId) {

        MovieFilter effectiveFilter = movieFilterValidator.orEmpty(movieFilter);
        movieFilterValidator.validate(effectiveFilter);

        return executeWithPagination(
                pageRequestContext,
                defaultPageSize,
                pageable -> movieService.getMovies(effectiveFilter, title, pageable, userId)
        );
    }

    public Page<MovieForUser<MovieCardViewDTO>> filterByGenre(
            String genre,
            PageRequestContext pageRequestContext,
            Long userId) {

        return executeWithPagination(
                pageRequestContext,
                genreDefaultPageSize,
                pageable -> movieService.getMoviesByGenre(genre, pageable, userId)
        );
    }

    private Page<MovieForUser<MovieCardViewDTO>> executeWithPagination(
            PageRequestContext pageRequestContext,
            int defaultSize,
            Function<Pageable, Page<MovieForUser<MovieCardViewDTO>>> fetchFunction) {

        Pageable pageable = movieSortResolver.resolve(pageRequestContext, defaultSize);

        Page<MovieForUser<MovieCardViewDTO>> result = fetchFunction.apply(pageable);

        int totalPages = result.getTotalPages();
        int requestedPage = pageable.getPageNumber();

        if (totalPages <= 0 || requestedPage < totalPages) {
            return result;
        }

        PageRequestContext lastPageRequestContext = PageRequestContext.builder()
                .sortBy(pageRequestContext.sortBy())
                .sortDirection(pageRequestContext.sortDirection())
                .page(totalPages - 1)
                .size(pageable.getPageSize())
                .build();

        Pageable lastPageable = movieSortResolver.resolve(lastPageRequestContext, defaultSize);
        return fetchFunction.apply(lastPageable);
    }
}