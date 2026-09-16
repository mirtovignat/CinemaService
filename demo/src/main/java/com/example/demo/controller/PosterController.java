package com.example.demo.controller;

import com.example.demo.service.movie.poster.MoviePosterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/posters")
@RequiredArgsConstructor
public class PosterController {

    private final MoviePosterService moviePosterService;

    @PostMapping("/fill-missing")
    public ResponseEntity<String> fillMissing() {
        try {
            int filled = moviePosterService.fillMissingPosters();
            return ResponseEntity.ok("Заполнено постеров: " + filled);
        } catch (Exception exception) {
            return ResponseEntity.internalServerError()
                    .body("Ошибка: " + exception.getMessage());
        }
    }
}