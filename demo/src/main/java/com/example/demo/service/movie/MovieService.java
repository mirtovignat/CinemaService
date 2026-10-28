package com.example.demo.service.movie;

import com.example.demo.dto.movie.*;
import com.example.demo.dto.movie.tmdb.MoviePriceItem;
import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.dto.movie.tmdb.TmdbSearchRequest;
import com.example.demo.dto.movie.tmdb.TmdbSearchResultDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.filter.MovieFilter;
import com.example.demo.filter.MovieSearchHelper;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.service.movie.poster.MoviePosterService;
import com.example.demo.service.movie.poster.TMDBService;
import com.example.demo.util.TitleMatcher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Service
public class MovieService {

    private static final int MAX_TMDB_RESULTS = 100;

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final MovieUserStatusBatchLoader movieUserStatusBatchLoader;
    private final MovieSearchHelper movieSearchHelper;
    private final MoviePosterService moviePosterService;
    private final TMDBService tmdbService;

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
    public Double getMovieRating(Long movieId) {
        return movieRepository.findRatingById(movieId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<MovieSearchDTO> getMovieSearchDTO(String movieSearchQuery) {
        return movieSearchHelper.getSuggestions(movieSearchQuery);
    }

    @Transactional(readOnly = true)
    public List<TmdbSearchResultDTO> searchTmdb(TmdbSearchRequest tmdbSearchRequest) {
        if (tmdbSearchRequest == null) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY);
        }

        Set<Long> intersectedTmdbIds;
        if (tmdbSearchRequest.hasAnyIds()) {
            intersectedTmdbIds = new LinkedHashSet<>(tmdbSearchRequest.tmdbIds());
        } else if (tmdbSearchRequest.hasAnyFields()) {
            intersectedTmdbIds = intersectByFields(tmdbSearchRequest);
        } else {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY);
        }

        if (intersectedTmdbIds.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY);
        }

        Set<Long> existingTmdbIds = movieRepository.findExistingTmdbIds(intersectedTmdbIds);
        Set<Long> existingTmdbIdSet = existingTmdbIds == null
                ? Set.of()
                : existingTmdbIds.stream()
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());

        List<Long> notInDatabaseTmdbIds = intersectedTmdbIds.stream()
                .filter(tmdbId -> !existingTmdbIdSet.contains(tmdbId))
                .toList();

        if (notInDatabaseTmdbIds.isEmpty()) {
            List<MovieRefer> movieReferList = movieRepository.findRefsByTmdbIds(existingTmdbIdSet)
                    .stream()
                    .map(movieReferProjection -> MovieRefer.builder()
                            .id(movieReferProjection.getId())
                            .title(movieReferProjection.getTitle())
                            .year(movieReferProjection.getReleaseDate() != null
                                    ? movieReferProjection.getReleaseDate().getYear()
                                    : null)
                            .build())
                    .toList();
            throw BusinessException.of(ErrorCode.TMDB_MOVIE_ALREADY_EXISTS, movieReferList);
        }

        Map<Long, TMDBMovieDTO> uniqueTmdbMovies = new LinkedHashMap<>();
        for (Long tmdbId : notInDatabaseTmdbIds) {
            TMDBMovieDTO tmdbMovieDTO = tmdbService.getMovieById(tmdbId);
            if (tmdbMovieDTO == null || tmdbMovieDTO.id() == null) {
                continue;
            }
            uniqueTmdbMovies.putIfAbsent(tmdbMovieDTO.id(), tmdbMovieDTO);
        }

        if (uniqueTmdbMovies.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY);
        }

        List<TMDBMovieDTO> sortedByDateDesc = uniqueTmdbMovies.values().stream()
                .sorted(Comparator.comparing(
                        (TMDBMovieDTO tmdbMovieDTO) -> tmdbMovieDTO.releaseDate() == null
                                ? ""
                                : tmdbMovieDTO.releaseDate()
                ).reversed())
                .limit(MAX_TMDB_RESULTS)
                .toList();

        return sortedByDateDesc.stream()
                .map(movieMapper::toTmdbSearchResult)
                .toList();
    }

    private Set<Long> intersectByFields(TmdbSearchRequest tmdbSearchRequest) {
        List<Set<Long>> criteriaSets = new ArrayList<>();

        if (tmdbSearchRequest.titles() != null && !tmdbSearchRequest.titles().isEmpty()) {
            Set<Long> titleMatches = new LinkedHashSet<>();
            for (String title : tmdbSearchRequest.titles()) {
                if (title == null || title.isBlank()) {
                    continue;
                }
                for (TMDBMovieDTO tmdbMovieDTO : tmdbService.searchMovies(title)) {
                    if (tmdbMovieDTO == null || tmdbMovieDTO.id() == null) {
                        continue;
                    }
                    if (!TitleMatcher.matchesAnyExactly(
                            title,
                            tmdbMovieDTO.title(),
                            tmdbMovieDTO.originalTitle())) {
                        continue;
                    }
                    titleMatches.add(tmdbMovieDTO.id());
                }
            }
            criteriaSets.add(titleMatches);
        }

        if (tmdbSearchRequest.directors() != null && !tmdbSearchRequest.directors().isEmpty()) {
            Set<Long> directorMatches = new LinkedHashSet<>();
            for (String director : tmdbSearchRequest.directors()) {
                if (director == null || director.isBlank()) {
                    continue;
                }
                for (TMDBMovieDTO tmdbMovieDTO : tmdbService.searchMoviesByDirector(director)) {
                    if (tmdbMovieDTO != null && tmdbMovieDTO.id() != null) {
                        directorMatches.add(tmdbMovieDTO.id());
                    }
                }
            }
            criteriaSets.add(directorMatches);
        }

        if (tmdbSearchRequest.actors() != null && !tmdbSearchRequest.actors().isEmpty()) {
            Set<Long> actorMatches = new LinkedHashSet<>();
            for (String actor : tmdbSearchRequest.actors()) {
                if (actor == null || actor.isBlank()) {
                    continue;
                }
                for (TMDBMovieDTO tmdbMovieDTO : tmdbService.searchMoviesByActor(actor)) {
                    if (tmdbMovieDTO != null && tmdbMovieDTO.id() != null) {
                        actorMatches.add(tmdbMovieDTO.id());
                    }
                }
            }
            criteriaSets.add(actorMatches);
        }

        if (criteriaSets.isEmpty()) {
            return Set.of();
        }

        Set<Long> intersectedTmdbIds = criteriaSets.get(0);
        for (int index = 1; index < criteriaSets.size(); index++) {
            intersectedTmdbIds.retainAll(criteriaSets.get(index));
        }
        return intersectedTmdbIds;
    }

    @Transactional
    public List<MovieDetailsViewDTO> createMovies(List<MoviePriceItem> moviePriceItems) {
        if (moviePriceItems == null || moviePriceItems.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
        if (moviePriceItems.size() > MAX_TMDB_RESULTS) {
            throw BusinessException.of(ErrorCode.TOO_MANY_TMDB_RESULTS,
                    moviePriceItems.size(), MAX_TMDB_RESULTS);
        }
        for (MoviePriceItem moviePriceItem : moviePriceItems) {
            if (moviePriceItem == null || moviePriceItem.tmdbId() == null) {
                throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
            }
            if (moviePriceItem.price() == null || moviePriceItem.price().signum() <= 0) {
                throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
            }
        }

        List<MovieDetailsViewDTO> createdMovieDetails = new ArrayList<>();
        for (MoviePriceItem moviePriceItem : moviePriceItems) {
            Long tmdbId = moviePriceItem.tmdbId();
            BigDecimal price = moviePriceItem.price();

            if (movieRepository.existsByTmdbId(tmdbId)) {
                throw BusinessException.of(ErrorCode.MOVIE_ALREADY_EXISTS);
            }

            TMDBMovieDTO tmdbMovieDTO = tmdbService.getMovieById(tmdbId);
            if (tmdbMovieDTO == null) {
                continue;
            }

            Movie movie = movieMapper.fromTmdb(tmdbMovieDTO, price);
            Movie savedMovie = movieRepository.save(movie);

            try {
                moviePosterService.refreshOne(savedMovie.getId());
            } catch (IllegalStateException posterException) {
                log.warn("Poster fetch failed for movie id={} tmdbId={}: {}",
                        savedMovie.getId(), tmdbId, posterException.getMessage());
            }

            createdMovieDetails.add(movieMapper.toDetails(movieRepository.findByIdOrThrow(savedMovie.getId())));
        }

        if (createdMovieDetails.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY);
        }

        return createdMovieDetails;
    }

    @Transactional(readOnly = true)
    public Page<MovieForUser<MovieCardViewDTO>> enrichWithStatuses(Page<Movie> moviePage,
                                                                   Long userId) {
        if (moviePage.isEmpty()) {
            return Page.empty(moviePage.getPageable());
        }
        if (userId != null) {
            List<Long> movieIds = moviePage.getContent().stream().map(Movie::getId).toList();
            Map<Long, MovieUserStatus> movieUserStatuses = movieUserStatusBatchLoader.loadForMovies(movieIds, userId);
            return moviePage.map(movie -> buildMovieForUser(
                    movie,
                    movieUserStatuses.getOrDefault(movie.getId(), MovieUserStatus.builder().build())
            ));
        }
        return moviePage.map(movie -> buildMovieForUser(
                movie,
                MovieUserStatus.builder().build()
        ));
    }

    public Movie getMovie(Long movieId) {
        return movieRepository.findByIdOrThrow(movieId);
    }

    private MovieForUser<MovieCardViewDTO> buildMovieForUser(Movie movie, MovieUserStatus movieUserStatus) {
        return MovieForUser.<MovieCardViewDTO>builder()
                .movie(movieMapper.toCard(movie))
                .status(movieUserStatus)
                .build();
    }
}