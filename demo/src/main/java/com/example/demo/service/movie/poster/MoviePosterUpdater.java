package com.example.demo.service.movie.poster;

import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MoviePosterUpdater {

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

        var tmdbMovie = tmdbService.getMovieById(tmdbId);
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
}