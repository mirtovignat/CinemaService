package com.example.demo.service.movie.poster;

import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePosterService {

    private final MovieRepository movieRepository;
    private final MoviePosterUpdater moviePosterUpdater;

    public int fillMissingPosters() {
        List<Movie> movies = movieRepository.findByPosterUrlIsNull();
        int filled = 0;
        for (Movie movie : movies) {
            if (movie.getTmdbId() == null) {
                continue;
            }
            try {
                moviePosterUpdater.replacePosterFromTMDB(movie.getId());
                filled++;
            } catch (Exception exception) {
                log.warn("Fail id={} tmdb_id={}: {}",
                        movie.getId(), movie.getTmdbId(), exception.getMessage());
            }
        }
        return filled;
    }
}