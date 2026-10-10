package com.example.demo.service.movie.poster;

import com.example.demo.dto.movie.TMDBMovieDTO;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoviePosterService {

    private final TMDBService tmdbService;
    private final PosterService posterService;
    private final MovieRepository movieRepository;

    @Transactional
    public void replacePosterFromTMDB(Long movieId) throws Exception {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Фильм не найден: " + movieId));

        Long tmdbId = movie.getTmdbId();
        if (tmdbId == null) {
            throw new IllegalStateException(
                    "У фильма нет tmdb_id: " + movie.getTitle());
        }

        TMDBMovieDTO tmdbMovie = tmdbService.getMovieById(tmdbId);
        if (tmdbMovie == null || tmdbMovie.posterPath() == null) {
            throw new IllegalStateException(
                    "Нет постера в TMDB для tmdb_id=" + tmdbId);
        }

        String tmdbPosterUrl = tmdbService.getPosterUrl(tmdbMovie.posterPath());
        String fileName = "movie-" + movie.getId() + ".jpg";
        String minioUrl = posterService.uploadPosterFromUrl(tmdbPosterUrl, fileName);

        movie.setPosterUrl(minioUrl);
        movieRepository.save(movie);
    }

    @Transactional
    public int fillMissingPosters() {
        List<Movie> movies = movieRepository.findByPosterUrlIsNull();
        int filled = 0;
        for (Movie movie : movies) {
            if (movie.getTmdbId() == null) {
                continue;
            }
            try {
                replacePosterFromTMDB(movie.getId());
                filled++;
            } catch (Exception exception) {
                log.warn("Fail id={} tmdb_id={}: {}",
                        movie.getId(), movie.getTmdbId(), exception.getMessage());
            }
        }
        return filled;
    }
}