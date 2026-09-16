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
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MovieSearchHelper {

    private final MovieRepository movieRepository;
    private final MovieSpecifications movieSpecifications;
    private final MovieMapper movieMapper;

    public Page<Movie> findMoviesByMovieFilter(MovieFilter movieFilter,
                                               Pageable pageable) {
        if (movieFilter == null) {
            return movieRepository.findAllWithActiveRatings(pageable);
        }
        return movieRepository.findAll(movieSpecifications.byFilter(movieFilter),
                pageable);
    }

    public List<MovieSearchDTO> getSuggestions(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }
        MovieFilter movieFilter = MovieFilter.builder()
                .title(query)
                .build();
        Pageable limit = PageRequest.of(0, 10);
        return movieRepository.findAll(movieSpecifications
                        .byFilter(movieFilter), limit)
                .getContent()
                .stream()
                .map(movieMapper::toMovieSearch)
                .toList();
    }
}