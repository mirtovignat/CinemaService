package com.example.demo.service.movie;

import com.example.demo.dto.movie.*;
import com.example.demo.filter.MovieFilter;
import com.example.demo.filter.MovieSearchHelper;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Service
public class MovieService {

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final MovieUserStatusBatchLoader movieUserStatusBatchLoader;
    private final MovieSearchHelper movieSearchHelper;

    @Transactional(readOnly = true)
    public Page<MovieForUser<MovieCardViewDTO>> getMovies(MovieFilter movieFilter,
                                                          String title,
                                                          Pageable pageable,
                                                          Long userId) {
        Page<Movie> moviePage = movieSearchHelper.findMovies(movieFilter, title, pageable);
        return enrichWithStatuses(moviePage, userId);
    }

    @Transactional(readOnly = true)
    public Page<MovieForUser<MovieCardViewDTO>> getMoviesByGenre(String genre,
                                                                 Pageable pageable,
                                                                 Long userId) {
        Page<Movie> moviePage = movieRepository.findAllByGenre(pageable, genre);
        return enrichWithStatuses(moviePage, userId);
    }

    @Transactional(readOnly = true)
    public MovieForUser<MovieDetailsViewDTO> getCard(Long movieId, Long userId) {
        Movie movie = movieRepository.findByIdOrThrow(movieId);
        MovieDetailsViewDTO movieDetailsViewDTO = movieMapper.toDetails(movie);
        MovieUserStatus movieUserStatus = (userId != null)
                ? movieUserStatusBatchLoader.loadForMovies(List.of(movieId), userId)
                .getOrDefault(movieId, MovieUserStatus.builder().build())
                : MovieUserStatus.builder().build();
        return MovieForUser.<MovieDetailsViewDTO>builder()
                .movie(movieDetailsViewDTO)
                .status(movieUserStatus)
                .build();
    }

    @Transactional(readOnly = true)
    public Double getMovieRating(Long id) {
        return movieRepository.findRatingById(id).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MovieSearchDTO> getMovieSearchDTO(String movieSearchQuery) {
        return movieSearchHelper.getSuggestions(movieSearchQuery);
    }

    @Transactional(readOnly = true)
    public Page<MovieForUser<MovieCardViewDTO>> enrichWithStatuses(Page<Movie> moviePage,
                                                                   Long userId) {
        if (moviePage.isEmpty()) {
            return Page.empty(moviePage.getPageable());
        }
        if (userId != null) {
            List<Long> movieIds = moviePage.getContent().stream()
                    .map(Movie::getId)
                    .toList();
            Map<Long, MovieUserStatus> statuses = movieUserStatusBatchLoader.loadForMovies(movieIds, userId);
            return moviePage.map(movie -> MovieForUser.<MovieCardViewDTO>builder()
                    .movie(movieMapper.toCard(movie))
                    .status(statuses.getOrDefault(movie.getId(), MovieUserStatus.builder().build()))
                    .build());
        }
        return moviePage.map(movie -> MovieForUser.<MovieCardViewDTO>builder()
                .movie(movieMapper.toCard(movie))
                .status(MovieUserStatus.builder().build())
                .build());
    }

    public Movie getMovie(Long movieId) {
        return movieRepository.findByIdOrThrow(movieId);
    }
}