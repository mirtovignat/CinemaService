package com.example.demo.controller;

import com.example.demo.dto.movie.tmdb.Outcome;
import com.example.demo.dto.movie.tmdb.RefreshAllResult;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.service.movie.poster.MoviePosterService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posters")
@AllArgsConstructor
public class PosterController {

    private final MoviePosterService moviePosterService;
    private final MovieRepository movieRepository;

    @PostMapping("/refresh-all")
    public ResponseEntity<RefreshAllResult> refreshAll() {
        return ResponseEntity.ok(moviePosterService.refreshAll());
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<String> refreshOne(@PathVariable Long movieId) {
        Outcome outcome = moviePosterService.refreshOne(movieId);

        return switch (outcome) {
            case OK -> {
                Movie movie = movieRepository.findByIdOrThrow(movieId);
                yield ResponseEntity.ok(movie.getPosterUrl());
            }
            case NO_TMDB -> ResponseEntity.badRequest()
                    .body("Movie " + movieId + " has no tmdbId");
            case FAILED -> ResponseEntity.internalServerError()
                    .body("Poster refresh failed for movie " + movieId);
        };
    }
}