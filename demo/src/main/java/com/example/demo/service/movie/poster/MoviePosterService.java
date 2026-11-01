package com.example.demo.service.movie.poster;

import com.example.demo.dto.movie.tmdb.Outcome;
import com.example.demo.dto.movie.tmdb.RefreshAllResult;
import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.service.movie.tmdb.TMDBService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MoviePosterService {

    private final MovieRepository movieRepository;
    private final MoviePosterWriter moviePosterWriter;
    private final TMDBService tmdbService;
    private final PosterService posterService;
    private final int refreshBatchSize;

    public MoviePosterService(MovieRepository movieRepository,
                              MoviePosterWriter moviePosterWriter,
                              TMDBService tmdbService,
                              PosterService posterService,
                              @Value("${app.tmdb.refresh-batch-size:500}") int refreshBatchSize) {
        this.movieRepository = movieRepository;
        this.moviePosterWriter = moviePosterWriter;
        this.tmdbService = tmdbService;
        this.posterService = posterService;
        this.refreshBatchSize = refreshBatchSize;
    }

    public RefreshAllResult refreshAll() {
        List<Movie> movieList = movieRepository
                .findAll(PageRequest.of(0, refreshBatchSize))
                .getContent();

        int updated = 0;
        int skipped = 0;
        int failed = 0;

        for (Movie movie : movieList) {
            switch (refresh(movie)) {
                case OK -> updated++;
                case NO_TMDB -> skipped++;
                case FAILED -> failed++;
            }
        }

        return RefreshAllResult.builder()
                .total(movieList.size())
                .skipped(skipped)
                .updated(updated)
                .failed(failed)
                .build();
    }

    public Outcome refreshOne(Long movieId) {
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        return refresh(movie);
    }

    private Outcome refresh(Movie movie) {
        Long tmdbId = movie.getTmdbId();
        if (tmdbId == null) {
            return Outcome.NO_TMDB;
        }
        try {
            TMDBMovieDTO tmdbMovieDTO = tmdbService.getMovieById(tmdbId);
            if (tmdbMovieDTO == null || tmdbMovieDTO.posterPath() == null || tmdbMovieDTO.posterPath().isBlank()) {
                return Outcome.FAILED;
            }
            String remoteUrl = tmdbService.getPosterUrl(tmdbMovieDTO.posterPath());
            String fileName = "movie-" + movie.getId() + ".jpg";
            String minioUrl = posterService.uploadPosterFromUrl(remoteUrl, fileName);
            moviePosterWriter.savePosterUrl(movie.getId(), minioUrl);
            return Outcome.OK;
        } catch (Exception exception) {
            return Outcome.FAILED;
        }
    }
}