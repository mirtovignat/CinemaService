package com.example.demo.filter;

import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
@AllArgsConstructor
public class MovieSearchHelper {

    private static final int SUGGESTIONS_LIMIT = 10;

    private final MovieRepository movieRepository;
    private final MovieSpecifications movieSpecifications;
    private final MovieMapper movieMapper;
    private final MovieFilterValidator movieFilterValidator;

    @Transactional(readOnly = true)
    public Page<Movie> findMovies(MovieFilter movieFilter,
                                  String title,
                                  Pageable pageable) {
        if (!movieFilterValidator.hasAnySearchCriteria(movieFilter, title)) {
            return movieRepository.findAll(pageable);
        }

        Specification<Movie> specification = Specification.allOf(
                movieSpecifications.byFilter(movieFilter),
                movieSpecifications.fieldStartsWith("title", title)
        );

        return movieRepository.findAll(specification, pageable);
    }

    @Transactional(readOnly = true)
    public List<MovieSearchDTO> getSuggestions(String searchQuery) {
        Optional<String> sanitized = movieFilterValidator.normalizeSearchQueryOptional(searchQuery);
        if (sanitized.isEmpty()) {
            return Collections.emptyList();
        }

        Specification<Movie> specification =
                movieSpecifications.fieldStartsWithNormalized("title", sanitized.get());

        List<Movie> movies = movieRepository
                .findAll(
                        specification,
                        PageRequest.of(
                                0,
                                SUGGESTIONS_LIMIT,
                                Sort.by(Sort.Direction.ASC, "title", "id")
                        )
                )
                .getContent();

        return movies.stream()
                .map(movieMapper::toMovieSearch)
                .toList();
    }
}