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

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Component
public class MovieSearchHelper {

    private final MovieRepository movieRepository;
    private final MovieSpecifications movieSpecifications;
    private final MovieMapper movieMapper;
    private final MovieFilterValidator movieFilterValidator;

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

    public List<MovieSearchDTO> getSuggestions(String searchQuery, int limit) {
        Optional<String> sanitized = movieFilterValidator.normalizeSearchQueryOptional(searchQuery);
        if (sanitized.isEmpty()) {
            return List.of();
        }

        Specification<Movie> specification =
                movieSpecifications.fieldStartsWithNormalized("title", sanitized.get());

        List<Movie> movies = movieRepository
                .findAll(
                        specification,
                        PageRequest.of(
                                0,
                                limit,
                                Sort.by(Sort.Direction.ASC, "title", "id")
                        )
                )
                .getContent();

        return movies.stream()
                .map(movieMapper::toMovieSearch)
                .toList();
    }
}