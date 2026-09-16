package com.example.demo.dto.response;

import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import lombok.Builder;

import java.util.List;

@Builder
public record PersonalAccountResponse(
        UserForOwnerViewDTO userForOwner,
        long purchasesCount,
        long ratingsCount,
        long favouritesCount,
        long cartCount,
        List<RatedMovieViewDTO> topThreeRatedMovies,
        List<MovieCardViewDTO> recommendedMovies,
        List<String> genres
) {
}