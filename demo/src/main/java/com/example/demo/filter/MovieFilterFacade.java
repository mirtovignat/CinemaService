package com.example.demo.filter;

import com.example.demo.config.properties.PaginationProperties;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.service.movie.MovieService;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@AllArgsConstructor
public class MovieFilterFacade {

    private final MovieService movieService;
    private final MovieSortResolver movieSortResolver;
    private final MovieFilterValidator movieFilterValidator;
    private final PaginationProperties paginationProperties;

    public Page<MovieForUser<MovieCardViewDTO>> filterMovies(
            MovieFilter movieFilter,
            String title,
            PageRequestContext pageRequestContext,
            Long userId) {

        MovieFilter effectiveFilter = movieFilterValidator.orEmpty(movieFilter);
        movieFilterValidator.validate(effectiveFilter);

        return executeWithPagination(
                pageRequestContext,
                paginationProperties.defaultSize(),
                pageable -> movieService.getMovies(effectiveFilter, title, pageable, userId)
        );
    }

    public Page<MovieForUser<MovieCardViewDTO>> filterByGenre(
            String genre,
            PageRequestContext pageRequestContext,
            Long userId) {

        return executeWithPagination(
                pageRequestContext,
                paginationProperties.genreDefaultSize(),
                pageable -> movieService.getMoviesByGenre(genre, pageable, userId)
        );
    }

    private Page<MovieForUser<MovieCardViewDTO>> executeWithPagination(
            PageRequestContext pageRequestContext,
            int defaultSize,
            Function<Pageable, Page<MovieForUser<MovieCardViewDTO>>> fetchFunction) {

        Pageable pageable = movieSortResolver.resolve(pageRequestContext, defaultSize);
        return fetchFunction.apply(pageable);
    }
}