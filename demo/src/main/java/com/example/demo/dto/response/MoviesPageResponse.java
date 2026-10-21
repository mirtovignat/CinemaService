package com.example.demo.dto.response;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.filter.MovieFilter;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record MoviesPageResponse(
        Page<MovieForUser<MovieCardViewDTO>> cardsPage,
        MovieFilter movieFilter,
        boolean isSearch
) {
}