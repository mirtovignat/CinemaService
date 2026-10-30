package com.example.demo.service.movie;

import com.example.demo.dto.movie.*;
import com.example.demo.dto.movie.tmdb.MoviePriceItem;
import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.dto.movie.tmdb.TMDBSearchRequest;
import com.example.demo.dto.movie.tmdb.TMDBSearchResultDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.filter.MovieFilter;
import com.example.demo.filter.MovieSearchHelper;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.mapper.util.TmdbMappingUtil;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.projection.MovieReferProjection;
import com.example.demo.service.movie.poster.MoviePosterService;
import com.example.demo.service.movie.poster.TMDBService;
import com.example.demo.util.TitleMatcher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@AllArgsConstructor
@Service
public class MovieService {

    private static final int MAX_TMDB_RESULTS = 100;
    private static final long TMDB_REQUEST_DELAY_MS = 150;

    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final TmdbMappingUtil tmdbMappingUtil;
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
    public List<TMDBSearchResultDTO> searchTmdb(TMDBSearchRequest tmdbSearchRequest) {
        if (tmdbSearchRequest == null) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }

        String criteriaDescription = tmdbSearchRequest.describeCriteria();
        Set<Long> intersectedTmdbIds;

        if (tmdbSearchRequest.hasAnyIds()) {
            intersectedTmdbIds = new LinkedHashSet<>(tmdbSearchRequest.tmdbIds());
        } else if (tmdbSearchRequest.hasAnyFields()) {
            intersectedTmdbIds = intersectByFields(tmdbSearchRequest);
        } else {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }

        if (intersectedTmdbIds.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY, criteriaDescription);
        }

        Map<Long, Long> existingByTmdbId = new LinkedHashMap<>();
        List<MovieReferProjection> existingRefs = movieRepository.findRefsByTmdbIds(intersectedTmdbIds);
        if (existingRefs != null) {
            for (MovieReferProjection p : existingRefs) {
                if (p.getTmdbId() != null) {
                    existingByTmdbId.put(p.getTmdbId(), p.getId());
                }
            }
        }

        Map<Long, TMDBMovieDTO> uniqueTmdbMovies = new LinkedHashMap<>();
        for (Long tmdbId : intersectedTmdbIds) {
            TMDBMovieDTO dto = tmdbService.getMovieById(tmdbId);
            if (dto == null || dto.id() == null) continue;
            uniqueTmdbMovies.putIfAbsent(dto.id(), dto);
            sleepForTmdb();
        }

        if (uniqueTmdbMovies.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY, criteriaDescription);
        }

        return uniqueTmdbMovies.values().stream()
                .sorted(Comparator.comparing(
                        (TMDBMovieDTO d) -> d.releaseDate() == null ? "" : d.releaseDate()
                ).reversed())
                .limit(MAX_TMDB_RESULTS)
                .map(dto -> {
                    TMDBSearchResultDTO base = movieMapper.toTmdbSearchResult(dto);
                    return TMDBSearchResultDTO.builder()
                            .tmdbId(base.tmdbId())
                            .title(base.title())
                            .originalTitle(base.originalTitle())
                            .releaseDate(base.releaseDate())
                            .posterUrl(base.posterUrl())
                            .existingMovieId(existingByTmdbId.get(base.tmdbId()))
                            .build();
                })
                .toList();
    }

    private Set<Long> intersectByFields(TMDBSearchRequest req) {
        List<Set<Long>> sets = new ArrayList<>();

        if (req.titles() != null && !req.titles().isEmpty()) {
            Set<Long> matches = new LinkedHashSet<>();
            boolean any = false;
            for (String title : req.titles()) {
                if (title == null || title.isBlank()) continue;
                any = true;
                String normalizedInput = TitleMatcher.normalizeAndStripPunctuation(title);
                for (TMDBMovieDTO dto : tmdbService.searchMovies(title)) {
                    if (dto == null || dto.id() == null) continue;
                    String normalizedRu = TitleMatcher.normalizeAndStripPunctuation(dto.title());
                    String normalizedOrig = TitleMatcher.normalizeAndStripPunctuation(dto.originalTitle());
                    if (normalizedInput.equals(normalizedRu) || normalizedInput.equals(normalizedOrig)) {
                        matches.add(dto.id());
                    }
                }
                sleepForTmdb();
            }
            if (any) sets.add(matches);
        }

        if (req.directors() != null && !req.directors().isEmpty()) {
            Set<Long> matches = new LinkedHashSet<>();
            boolean any = false;
            for (String director : req.directors()) {
                if (director == null || director.isBlank()) continue;
                any = true;
                for (TMDBMovieDTO dto : tmdbService.searchMoviesByDirector(director)) {
                    if (dto != null && dto.id() != null) matches.add(dto.id());
                }
                sleepForTmdb();
            }
            if (any) sets.add(matches);
        }

        if (req.actors() != null && !req.actors().isEmpty()) {
            List<Set<Long>> perActor = new ArrayList<>();
            for (String actor : req.actors()) {
                if (actor == null || actor.isBlank()) continue;
                Set<Long> matches = new LinkedHashSet<>();
                for (TMDBMovieDTO dto : tmdbService.searchMoviesByActor(actor)) {
                    if (dto != null && dto.id() != null) matches.add(dto.id());
                }
                perActor.add(matches);
                sleepForTmdb();
            }
            if (!perActor.isEmpty()) {
                Set<Long> intersection = new LinkedHashSet<>(perActor.get(0));
                for (int i = 1; i < perActor.size(); i++) intersection.retainAll(perActor.get(i));
                sets.add(intersection);
            }
        }

        if (sets.isEmpty()) return Set.of();
        Set<Long> result = new LinkedHashSet<>(sets.get(0));
        for (int i = 1; i < sets.size(); i++) result.retainAll(sets.get(i));
        return result;
    }

    private void sleepForTmdb() {
        try { Thread.sleep(TMDB_REQUEST_DELAY_MS); }
        catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    @Transactional
    public List<MovieDetailsViewDTO> createMovies(List<MoviePriceItem> items) {
        if (items == null || items.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
        if (items.size() > MAX_TMDB_RESULTS) {
            throw BusinessException.of(ErrorCode.TOO_MANY_TMDB_RESULTS, items.size(), MAX_TMDB_RESULTS);
        }
        for (MoviePriceItem item : items) {
            if (item == null || item.tmdbId() == null) {
                throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
            }
            if (item.price() == null || item.price().signum() <= 0) {
                throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
            }
        }

        List<MovieDetailsViewDTO> created = new ArrayList<>();
        for (MoviePriceItem item : items) {
            Long tmdbId = item.tmdbId();
            if (movieRepository.existsByTmdbId(tmdbId)) {
                throw BusinessException.of(ErrorCode.MOVIE_ALREADY_EXISTS);
            }
            TMDBMovieDTO dto = tmdbService.getMovieById(tmdbId);
            if (dto == null) continue;

            String cert = tmdbService.getCertification(tmdbId);
            Integer ageRating = tmdbMappingUtil.certificationToAge(cert);

            Movie movie = movieMapper.fromTmdb(dto, item.price(), ageRating);
            Movie saved;
            try {
                saved = movieRepository.saveAndFlush(movie);
            } catch (DataIntegrityViolationException e) {
                log.warn("Concurrent insert for tmdbId={}", tmdbId);
                throw BusinessException.of(ErrorCode.MOVIE_ALREADY_EXISTS);
            }

            try {
                moviePosterService.refreshOne(saved.getId());
            } catch (Exception e) {
                log.warn("Poster refresh failed for movie id={} tmdbId={}: {}",
                        saved.getId(), tmdbId, e.getMessage());
            }

            created.add(movieMapper.toDetails(movieRepository.findByIdOrThrow(saved.getId())));
            sleepForTmdb();
        }

        if (created.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY, "ID TMDB");
        }
        return created;
    }

    @Transactional(readOnly = true)
    public Page<MovieForUser<MovieCardViewDTO>> enrichWithStatuses(Page<Movie> moviePage, Long userId) {
        if (moviePage.isEmpty()) return Page.empty(moviePage.getPageable());
        if (userId != null) {
            List<Long> ids = moviePage.getContent().stream().map(Movie::getId).toList();
            Map<Long, MovieUserStatus> statuses = movieUserStatusBatchLoader.loadForMovies(ids, userId);
            return moviePage.map(m -> buildMovieForUser(m, statuses.getOrDefault(m.getId(), MovieUserStatus.builder().build())));
        }
        return moviePage.map(m -> buildMovieForUser(m, MovieUserStatus.builder().build()));
    }

    public Movie getMovie(Long movieId) {
        return movieRepository.findByIdOrThrow(movieId);
    }

    private MovieForUser<MovieCardViewDTO> buildMovieForUser(Movie movie, MovieUserStatus status) {
        return MovieForUser.<MovieCardViewDTO>builder()
                .movie(movieMapper.toCard(movie))
                .status(status)
                .build();
    }
}