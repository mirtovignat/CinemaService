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

        Pageable pageable = sortResolver.resolve(page, size, context.sortBy(), context.sortDir());

        Page<MovieForUser<MovieCardViewDTO>> result =
                movieService.getMovies(filter, context.title(), pageable, userId);

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

        Pageable pageable = sortResolver.resolve(page, size, sortBy, sortDir);

        Page<MovieForUser<MovieCardViewDTO>> result =
                movieService.getMoviesByGenre(genre, pageable, userId);

        MovieSearchContext context = MovieSearchContext.ofGenre(genre, sortBy, sortDir);
        String filterQuery = queryBuilder.build(null, context);

        return Optional.of(new Result(result, filterQuery));
    }
}