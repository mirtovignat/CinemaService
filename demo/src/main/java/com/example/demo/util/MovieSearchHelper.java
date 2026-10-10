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

    private final MovieRepository movieRepository;
    private final MovieSpecifications movieSpecifications;
    private final MovieMapper movieMapper;

    public Page<Movie> findMovies(MovieFilter movieFilter,
                                  String title,
                                  Pageable pageable) {
        boolean hasFilter = movieFilter != null && movieFilter.hasAnyCriteria();
        boolean hasTitle = title != null && !title.isBlank();

        if (!hasFilter && !hasTitle) {
            return movieRepository.findAllWithActiveRatings(pageable);
        }

        Specification<Movie> spec = Specification.unrestricted();
        if (hasFilter) {
            spec = spec.and(movieSpecifications.byFilter(movieFilter));
        }
        if (hasTitle) {
            spec = spec.and(movieSpecifications.titleStartsWith(title));
        }
        return movieRepository.findAll(spec, pageable);
    }

    public List<MovieSearchDTO> getSuggestions(String query) {
        if (query == null || query.isBlank()) {
            return Collections.emptyList();
        }

        String sanitized = query.toLowerCase()
                .replace('ё', 'е')
                .replaceAll("[^a-zа-я0-9]", "");

        if (sanitized.isEmpty()) {
            return Collections.emptyList();
        }

        Pageable limit = PageRequest.of(0, 10);
        return movieRepository.findAll(
                        movieSpecifications.titleStartsWith(sanitized),
                        limit
                )
                .getContent()
                .stream()
                .map(movieMapper::toMovieSearch)
                .toList();
    }
}