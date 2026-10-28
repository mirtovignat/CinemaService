package com.example.demo.controller;

import com.example.demo.dto.tmdb.RefreshAllResult;
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

    @PostMapping("/refresh-all")
    public ResponseEntity<RefreshAllResult> refreshAll() {
        return ResponseEntity.ok(moviePosterService.refreshAll());
    }

    @PostMapping("/{movieId}")
    public ResponseEntity<String> refreshOne(@PathVariable Long movieId) {
        return ResponseEntity.ok(moviePosterService.refreshOne(movieId));
    }
}