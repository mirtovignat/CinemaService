package com.example.demo.filter;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.service.movie.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class MovieFilterFacade {

    private final MovieService movieService;
    private final MovieSortResolver sortResolver;
    private final MovieFilterQueryBuilder queryBuilder;
    private final GenreValidator genreValidator;

    public record Result(
            Page<MovieForUser<MovieCardViewDTO>> page,
            String filterQuery
    ) {
    }

    public Result filter(MovieFilter filter,
                         MovieSearchContext context,
                         int page,
                         int size,
                         Long userId) {

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 35;

        Pageable pageable = sortResolver.resolve(safePage, safeSize, context.sortBy(), context.sortDir());

        Page<MovieForUser<MovieCardViewDTO>> result =
                movieService.getMovies(filter, context.title(), pageable, userId);

        result = clampToLastExistingPage(
                result,
                safePage,
                lastPage -> {
                    Pageable lastPageable = sortResolver.resolve(
                            lastPage, safeSize, context.sortBy(), context.sortDir());
                    return movieService.getMovies(filter, context.title(), lastPageable, userId);
                }
        );

        String filterQuery = queryBuilder.build(filter, context);

        return new Result(result, filterQuery);
    }

    public Optional<Result> filterByGenre(String genre,
                                          String sortBy,
                                          String sortDir,
                                          int page,
                                          int size,
                                          Long userId) {

        if (!genreValidator.isValid(genre)) {
            return Optional.empty();
        }

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 15;

        Pageable pageable = sortResolver.resolve(safePage, safeSize, sortBy, sortDir);

        Page<MovieForUser<MovieCardViewDTO>> result =
                movieService.getMoviesByGenre(genre, pageable, userId);

        result = clampToLastExistingPage(
                result,
                safePage,
                lastPage -> {
                    Pageable lastPageable = sortResolver.resolve(lastPage, safeSize, sortBy, sortDir);
                    return movieService.getMoviesByGenre(genre, lastPageable, userId);
                }
        );

        MovieSearchContext context = MovieSearchContext.ofGenre(genre, sortBy, sortDir);
        String filterQuery = queryBuilder.build(null, context);

        return Optional.of(new Result(result, filterQuery));
    }

    private Page<MovieForUser<MovieCardViewDTO>> clampToLastExistingPage(
            Page<MovieForUser<MovieCardViewDTO>> result,
            int requestedPage,
            java.util.function.IntFunction<Page<MovieForUser<MovieCardViewDTO>>> reloadWithPage
    ) {
        int totalPages = result.getTotalPages();
        if (totalPages <= 0) {
            return result;
        }
        if (requestedPage < totalPages) {
            return result;
        }
        int lastPage = totalPages - 1;
        return reloadWithPage.apply(lastPage);
    }
}