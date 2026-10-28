package com.example.demo.service.movie.poster;

import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.dto.tmdb.Outcome;
import com.example.demo.dto.tmdb.RefreshAllResult;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class MoviePosterService {

    private final MovieRepository movieRepository;
    private final MoviePosterWriter moviePosterWriter;
    private final TMDBService tmdbService;
    private final PosterService posterService;

    public RefreshAllResult refreshAll() {
        List<Movie> movies = movieRepository.findAll();
        int updated = 0, skipped = 0, failed = 0;
        for (Movie movie : movies) {
            switch (refresh(movie)) {
                case OK -> updated++;
                case NO_TMDB -> skipped++;
                case FAILED -> failed++;
            }
        }
        return RefreshAllResult.builder()
                .total(movies.size())
                .skipped(skipped)
                .updated(updated)
                .failed(failed)
                .build();
    }

    public String refreshOne(Long movieId) {
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        return switch (refresh(movie)) {
            case OK -> movieRepository.findById(movieId).orElseThrow().getPosterUrl();
            case NO_TMDB -> throw new IllegalStateException(
                    "Movie " + movieId + " has no tmdbId");
            case FAILED -> throw new IllegalStateException(
                    "Poster refresh failed for movie " + movieId);
        };
    }

    private Outcome refresh(Movie movie) {
        Long tmdbId = movie.getTmdbId();
        if (tmdbId == null) {
            log.debug("Skip movie id={}: no tmdbId", movie.getId());
            return Outcome.NO_TMDB;
        }
        try {
            TMDBMovieDTO tmdbMovieDTO = tmdbService.getMovieById(tmdbId);
            if (tmdbMovieDTO == null || tmdbMovieDTO.posterPath() == null || tmdbMovieDTO.posterPath().isBlank()) {
                log.warn("TMDB has no poster: movie id={} tmdbId={}", movie.getId(), tmdbId);
                return Outcome.FAILED;
            }
            String remoteUrl = tmdbService.getPosterUrl(tmdbMovieDTO.posterPath());
            String fileName = "movie-" + movie.getId() + ".jpg";
            String minioUrl = posterService.uploadPosterFromUrl(remoteUrl, fileName);
            moviePosterWriter.savePosterUrl(movie.getId(), minioUrl);
            return Outcome.OK;
        } catch (Exception exception) {
            log.warn("Poster refresh failed: movie id={} tmdbId={} — {}",
                    movie.getId(), tmdbId, exception.getMessage());
            return Outcome.FAILED;
        }
    }
}