package com.example.demo.service.movie.poster;

import com.example.demo.repository.entity.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoviePosterWriter {

    private final MovieRepository movieRepository;

    @Transactional
    public void savePosterUrl(Long movieId, String url) {
        movieRepository.updatePosterUrl(movieId, url);
    }
}