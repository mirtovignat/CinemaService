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
                              StringKeysetPaginator stringKeysetPaginator,
                              FiltersProperties filtersProperties) {
        super(stringKeysetPaginator, filtersProperties.pageSize());
        this.movieRepository = movieRepository;
    }

    @Override
    @Transactional(readOnly = true)
    protected List<String> fetchFirstPage(String searchPattern, int limit) {
        return movieRepository.fetchGenresKeysetFirstPage(searchPattern, limit);
    }

    @Override
    @Transactional(readOnly = true)
    protected List<String> fetchWithCursor(String searchPattern, String cursor, int limit) {
        return movieRepository.fetchGenresKeysetWithCursor(searchPattern, cursor, limit);
    }
}