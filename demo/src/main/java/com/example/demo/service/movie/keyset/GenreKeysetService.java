package com.example.demo.service.movie.keyset;

import com.example.demo.config.properties.FiltersProperties;
import com.example.demo.repository.entity.MovieRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GenreKeysetService extends AbstractStringKeysetService {

    private final MovieRepository movieRepository;

    public GenreKeysetService(MovieRepository movieRepository,
                              StringKeysetPaginator paginator,
                              FiltersProperties filtersProperties) {
        super(paginator, filtersProperties.pageSize());
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional(readOnly = true)
    protected List<String> fetchFirstPage(String normalizedQuery, int limit) {
        return movieRepository.fetchGenresKeysetFirstPage(normalizedQuery, limit);
    }

    @Override
    @Transactional(readOnly = true)
    protected List<String> fetchWithCursor(String normalizedQuery, String cursor, int limit) {
        return movieRepository.fetchGenresKeysetWithCursor(normalizedQuery, cursor, limit);
    }
}