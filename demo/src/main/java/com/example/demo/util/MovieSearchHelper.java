package com.example.demo.util;

import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.filter.MovieFilter;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.specification.MovieSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MovieSearchHelper {

    private static final int SUGGESTIONS_LIMIT = 10;

    private final MovieRepository movieRepository;
    private final MovieSpecifications movieSpecifications;
    private final MovieMapper movieMapper;

    public Page<Movie> findMovies(MovieFilter movieFilter,
                                  String title,
                                  Pageable pageable) {
        boolean hasFilter = movieFilter != null && movieFilter.hasAnyCriteria();
        boolean hasTitle = title != null && !title.isBlank();

        if (!hasFilter && !hasTitle) {
            return movieRepository.findAllPaged(pageable);
        }

        Specification<Movie> specification = Specification.allOf(
                movieSpecifications.byFilter(movieFilter),
                movieSpecifications.titleStartsWith(title)
        );
        return movieRepository.findAll(specification, pageable);
    }

    public List<MovieSearchDTO> getSuggestions(String searchQuery) {
        if (searchQuery == null || searchQuery.isBlank()) {
            return Collections.emptyList();
        }

        String sanitizedSearchQuery = TitleNormalizer.normalize(searchQuery);

        if (sanitizedSearchQuery.isEmpty()) {
            return Collections.emptyList();
        }

        Pageable limitPageable = PageRequest.of(0, SUGGESTIONS_LIMIT);
        return movieRepository.findAll(
                        movieSpecifications.titleStartsWith(sanitizedSearchQuery),
                        limitPageable
                )
                .getContent()
                .stream()
                .map(movieMapper::toMovieSearch)
                .toList();
    }
}