package com.example.demo.filter;

import com.example.demo.service.movie.FilterMetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class GenreValidator {

    private final FilterMetaService filterMetaService;

    public boolean isValid(String genre) {
        return genre != null
                && !genre.isBlank()
                && filterMetaService.getAllGenres().contains(genre);
    }
}