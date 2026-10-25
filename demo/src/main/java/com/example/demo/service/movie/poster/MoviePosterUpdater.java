package com.example.demo.service.movie.poster;

import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class MoviePosterUpdater {

    private final TMDBService tmdbService;
    private final PosterService posterService;
    private final MovieRepository movieRepository;

    public void replacePosterFromTMDB(Long movieId) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Фильм не найден: " + movieId));
        Long tmdbId = movie.getTmdbId();
        if (tmdbId == null) {
            throw new IllegalStateException("У фильма нет tmdb_id: " + movie.getTitle());
        }

        var tmdbMovie = tmdbService.getMovieById(tmdbId);
        if (tmdbMovie == null || tmdbMovie.posterPath() == null) {
            throw new IllegalStateException("Нет постера в TMDB для tmdb_id=" + tmdbId);
        }

        String tmdbPosterUrl = tmdbService.getPosterUrl(tmdbMovie.posterPath());
        String fileName = "movie-" + movie.getId() + ".jpg";
        String minioUrl = posterService.uploadPosterFromUrl(tmdbPosterUrl, fileName);

        updatePosterUrl(movieId, minioUrl);
    }

    @Transactional
    public void updatePosterUrl(Long movieId, String posterUrl) {
        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new IllegalArgumentException("Фильм не найден: " + movieId));
        movie.setPosterUrl(posterUrl);
        movieRepository.save(movie);
    }
}