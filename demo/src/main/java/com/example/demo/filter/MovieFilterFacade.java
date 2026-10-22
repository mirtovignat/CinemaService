package com.example.demo.filter;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.service.movie.MovieService;
import lombok.Builder;
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

    @Builder
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

        Pageable pageable = sortResolver.resolve(safePage, safeSize, context.sortBy(), context.sortDirection());

        Page<MovieForUser<MovieCardViewDTO>> result =
                movieService.getMovies(filter, context.title(), pageable, userId);

        result = clampToLastExistingPage(
                result,
                safePage,
                lastPage -> {
                    Pageable lastPageable = sortResolver.resolve(
                            lastPage, safeSize, context.sortBy(), context.sortDirection());
                    return movieService.getMovies(filter, context.title(), lastPageable, userId);
                }
        );

        String filterQuery = queryBuilder.build(filter, context);

        return new Result(result, filterQuery);
    }

    public Optional<Result> filterByGenre(String genre,
                                          String sortBy,
                                          String sortDirection,
                                          int page,
                                          int size,
                                          Long userId) {

        if (!genreValidator.isValid(genre)) {
            return Optional.empty();
        }

        int safePage = Math.max(page, 0);
        int safeSize = size > 0 ? size : 15;

        Pageable pageable = sortResolver.resolve(safePage, safeSize, sortBy, sortDirection);

        Page<MovieForUser<MovieCardViewDTO>> result =
                movieService.getMoviesByGenre(genre, pageable, userId);

        result = clampToLastExistingPage(
                result,
                safePage,
                lastPage -> {
                    Pageable lastPageable = sortResolver.resolve(lastPage, safeSize, sortBy, sortDirection);
                    return movieService.getMoviesByGenre(genre, lastPageable, userId);
                }
        );

        MovieSearchContext movieSearchContext = MovieSearchContext.ofGenre(genre, sortBy, sortDirection);
        String filterQuery = queryBuilder.build(null, movieSearchContext);

        return Optional.of(Result.builder()
                .page(result)
                .filterQuery(filterQuery)
                .build());
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