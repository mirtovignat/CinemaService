package com.example.demo.dto.response;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import lombok.Builder;
import org.springframework.data.domain.Page;

@Builder
public record GenreMoviesPageResponse(
        Page<MovieForUser<MovieCardViewDTO>> cardsPage,
        String genre,
        boolean isSearch
) {
}