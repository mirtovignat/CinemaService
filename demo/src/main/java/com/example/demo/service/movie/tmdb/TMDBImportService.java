package com.example.demo.service.movie.tmdb;

import com.example.demo.dto.movie.MovieDetailsViewDTO;
import com.example.demo.dto.movie.tmdb.MoviePriceItem;
import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.dto.movie.tmdb.search.TMDBSearchRequest;
import com.example.demo.dto.movie.tmdb.search.TMDBSearchResultDTO;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.mapper.entity.MovieMapper;
import com.example.demo.mapper.util.TmdbMappingUtil;
import com.example.demo.model.entity.Movie;
import com.example.demo.repository.entity.MovieRepository;
import com.example.demo.repository.projection.MovieReferProjection;
import com.example.demo.service.movie.poster.MoviePosterService;
import com.example.demo.util.TitleMatcher;
import lombok.AllArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@AllArgsConstructor
public class TMDBImportService {

    private static final int MAX_TMDB_RESULTS = 100;

    private final TMDBService tmdbService;
    private final MovieRepository movieRepository;
    private final MovieMapper movieMapper;
    private final TmdbMappingUtil tmdbMappingUtil;
    private final MoviePosterService moviePosterService;

    @Transactional(readOnly = true)
    public List<TMDBSearchResultDTO> search(TMDBSearchRequest tmdbSearchRequest) {
        if (tmdbSearchRequest == null) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }

        String criteriaDescription = tmdbSearchRequest.describeCriteria();
        Set<Long> tmdbMovieIdSet = resolveCandidateTmdbIds(tmdbSearchRequest, criteriaDescription);

        if (tmdbMovieIdSet.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY, criteriaDescription);
        }

        Map<Long, Long> existingMovieIdMap = loadExistingMovieIds(tmdbMovieIdSet);
        Map<Long, TMDBMovieDTO> tmdbMovieDTOMap = loadUniqueMovies(tmdbMovieIdSet, criteriaDescription);

        return tmdbMovieDTOMap.values().stream()
                .sorted(Comparator.comparing(
                        (TMDBMovieDTO tmdbMovieDTO) -> tmdbMovieDTO.releaseDate() == null
                                ? ""
                                : tmdbMovieDTO.releaseDate()
                ).reversed())
                .limit(MAX_TMDB_RESULTS)
                .map(tmdbMovieDTO -> toSearchResult(tmdbMovieDTO, existingMovieIdMap))
                .toList();
    }

    @Transactional
    public List<MovieDetailsViewDTO> createMovies(List<MoviePriceItem> moviePriceItemList) {
        validateCreateItems(moviePriceItemList);

        List<MovieDetailsViewDTO> movieDetailsViewDTOList = new ArrayList<>();
        for (MoviePriceItem moviePriceItem : moviePriceItemList) {
            MovieDetailsViewDTO movieDetailsViewDTO = createOne(moviePriceItem);
            if (movieDetailsViewDTO != null) {
                movieDetailsViewDTOList.add(movieDetailsViewDTO);
            }
            tmdbService.pauseBetweenRequests();
        }

        if (movieDetailsViewDTOList.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY, "ID TMDB");
        }
        return movieDetailsViewDTOList;
    }

    private Set<Long> resolveCandidateTmdbIds(TMDBSearchRequest tmdbSearchRequest,
                                              String criteriaDescription) {
        if (tmdbSearchRequest.hasAnyIds()) {
            return new LinkedHashSet<>(tmdbSearchRequest.tmdbIds());
        }
        if (tmdbSearchRequest.hasAnyFields()) {
            return intersectByFields(tmdbSearchRequest);
        }
        throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
    }

    private Set<Long> intersectByFields(TMDBSearchRequest tmdbSearchRequest) {
        List<Set<Long>> tmdbMovieIdSetList = new ArrayList<>();
        addIfNotEmpty(tmdbMovieIdSetList, searchByTitles(tmdbSearchRequest.titles()));
        addIfNotEmpty(tmdbMovieIdSetList, searchByDirectors(tmdbSearchRequest.directors()));
        addIfNotEmpty(tmdbMovieIdSetList, searchByActors(tmdbSearchRequest.actors()));

        if (tmdbMovieIdSetList.isEmpty()) {
            return Set.of();
        }
        return intersectAll(tmdbMovieIdSetList);
    }

    private Set<Long> searchByTitles(List<String> titleList) {
        if (titleList == null || titleList.isEmpty()) {
            return Set.of();
        }
        Set<Long> matchedTmdbMovieIdSet = new LinkedHashSet<>();
        for (String title : titleList) {
            if (title == null || title.isBlank()) {
                continue;
            }
            String normalizedInput = TitleMatcher.normalizeAndStripPunctuation(title);
            for (TMDBMovieDTO tmdbMovieDTO : tmdbService.searchMovies(title)) {
                if (tmdbMovieDTO == null || tmdbMovieDTO.id() == null) {
                    continue;
                }
                String normalizedRu = TitleMatcher.normalizeAndStripPunctuation(tmdbMovieDTO.title());
                String normalizedOriginal = TitleMatcher.normalizeAndStripPunctuation(tmdbMovieDTO.originalTitle());
                if (normalizedInput.equals(normalizedRu) || normalizedInput.equals(normalizedOriginal)) {
                    matchedTmdbMovieIdSet.add(tmdbMovieDTO.id());
                }
            }
            tmdbService.pauseBetweenRequests();
        }
        return matchedTmdbMovieIdSet;
    }

    private Set<Long> searchByDirectors(List<String> directorList) {
        if (directorList == null || directorList.isEmpty()) {
            return Set.of();
        }
        Set<Long> matchedTmdbMovieIdSet = new LinkedHashSet<>();
        for (String director : directorList) {
            if (director == null || director.isBlank()) {
                continue;
            }
            for (TMDBMovieDTO tmdbMovieDTO : tmdbService.searchMoviesByDirector(director)) {
                if (tmdbMovieDTO != null && tmdbMovieDTO.id() != null) {
                    matchedTmdbMovieIdSet.add(tmdbMovieDTO.id());
                }
            }
            tmdbService.pauseBetweenRequests();
        }
        return matchedTmdbMovieIdSet;
    }

    private Set<Long> searchByActors(List<String> actorList) {
        if (actorList == null || actorList.isEmpty()) {
            return Set.of();
        }
        List<Set<Long>> perActorTmdbMovieIdSetList = new ArrayList<>();
        for (String actor : actorList) {
            if (actor == null || actor.isBlank()) {
                continue;
            }
            Set<Long> matchedTmdbMovieIdSet = new LinkedHashSet<>();
            for (TMDBMovieDTO tmdbMovieDTO : tmdbService.searchMoviesByActor(actor)) {
                if (tmdbMovieDTO != null && tmdbMovieDTO.id() != null) {
                    matchedTmdbMovieIdSet.add(tmdbMovieDTO.id());
                }
            }
            perActorTmdbMovieIdSetList.add(matchedTmdbMovieIdSet);
            tmdbService.pauseBetweenRequests();
        }
        return perActorTmdbMovieIdSetList.isEmpty() ? Set.of() : intersectAll(perActorTmdbMovieIdSetList);
    }

    private void addIfNotEmpty(List<Set<Long>> tmdbMovieIdSetList, Set<Long> candidateTmdbMovieIdSet) {
        if (candidateTmdbMovieIdSet != null && !candidateTmdbMovieIdSet.isEmpty()) {
            tmdbMovieIdSetList.add(candidateTmdbMovieIdSet);
        }
    }

    private Set<Long> intersectAll(List<Set<Long>> tmdbMovieIdSetList) {
        Set<Long> resultTmdbMovieIdSet = new LinkedHashSet<>(tmdbMovieIdSetList.get(0));
        for (int index = 1; index < tmdbMovieIdSetList.size(); index++) {
            resultTmdbMovieIdSet.retainAll(tmdbMovieIdSetList.get(index));
        }
        return resultTmdbMovieIdSet;
    }

    private Map<Long, Long> loadExistingMovieIds(Set<Long> tmdbMovieIdSet) {
        Map<Long, Long> existingMovieIdMap = new LinkedHashMap<>();
        List<MovieReferProjection> movieReferProjectionList = movieRepository.findRefsByTmdbIds(tmdbMovieIdSet);
        if (movieReferProjectionList == null) {
            return existingMovieIdMap;
        }
        for (MovieReferProjection movieReferProjection : movieReferProjectionList) {
            if (movieReferProjection.getTmdbId() != null) {
                existingMovieIdMap.put(movieReferProjection.getTmdbId(), movieReferProjection.getId());
            }
        }
        return existingMovieIdMap;
    }

    private Map<Long, TMDBMovieDTO> loadUniqueMovies(Set<Long> tmdbMovieIdSet, String criteriaDescription) {
        Map<Long, TMDBMovieDTO> tmdbMovieDTOMap = new LinkedHashMap<>();
        for (Long tmdbId : tmdbMovieIdSet) {
            TMDBMovieDTO tmdbMovieDTO = tmdbService.getMovieById(tmdbId);
            if (tmdbMovieDTO != null && tmdbMovieDTO.id() != null) {
                tmdbMovieDTOMap.putIfAbsent(tmdbMovieDTO.id(), tmdbMovieDTO);
            }
            tmdbService.pauseBetweenRequests();
        }
        if (tmdbMovieDTOMap.isEmpty()) {
            throw BusinessException.of(ErrorCode.TMDB_SEARCH_EMPTY, criteriaDescription);
        }
        return tmdbMovieDTOMap;
    }

    private TMDBSearchResultDTO toSearchResult(TMDBMovieDTO tmdbMovieDTO, Map<Long, Long> existingMovieIdMap) {
        TMDBSearchResultDTO baseTMDBSearchResultDTO = movieMapper.toTmdbSearchResult(tmdbMovieDTO);
        return TMDBSearchResultDTO.builder()
                .tmdbId(baseTMDBSearchResultDTO.tmdbId())
                .title(baseTMDBSearchResultDTO.title())
                .originalTitle(baseTMDBSearchResultDTO.originalTitle())
                .releaseDate(baseTMDBSearchResultDTO.releaseDate())
                .posterUrl(baseTMDBSearchResultDTO.posterUrl())
                .existingMovieId(existingMovieIdMap.get(baseTMDBSearchResultDTO.tmdbId()))
                .build();
    }

    private void validateCreateItems(List<MoviePriceItem> moviePriceItemList) {
        if (moviePriceItemList == null || moviePriceItemList.isEmpty()) {
            throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
        }
        if (moviePriceItemList.size() > MAX_TMDB_RESULTS) {
            throw BusinessException.of(ErrorCode.TOO_MANY_TMDB_RESULTS,
                    moviePriceItemList.size(), MAX_TMDB_RESULTS);
        }
        for (MoviePriceItem moviePriceItem : moviePriceItemList) {
            if (moviePriceItem == null || moviePriceItem.tmdbId() == null) {
                throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
            }
            if (moviePriceItem.price() == null || moviePriceItem.price().signum() <= 0) {
                throw BusinessException.of(ErrorCode.VALIDATION_ERROR);
            }
        }
    }

    private MovieDetailsViewDTO createOne(MoviePriceItem moviePriceItem) {
        Long tmdbId = moviePriceItem.tmdbId();
        if (movieRepository.existsByTmdbId(tmdbId)) {
            throw BusinessException.of(ErrorCode.MOVIE_ALREADY_EXISTS);
        }

        TMDBMovieDTO tmdbMovieDTO = tmdbService.getMovieById(tmdbId);
        if (tmdbMovieDTO == null) {
            return null;
        }

        Integer ageRating = tmdbMappingUtil.certificationToAge(
                tmdbService.extractCertification(tmdbMovieDTO));

        Movie movie = movieMapper.fromTmdb(tmdbMovieDTO, moviePriceItem.price(), ageRating);
        Movie savedMovie;
        try {
            savedMovie = movieRepository.saveAndFlush(movie);
        } catch (DataIntegrityViolationException dataIntegrityViolationException) {
            throw BusinessException.of(ErrorCode.MOVIE_ALREADY_EXISTS);
        }

        moviePosterService.refreshOne(savedMovie.getId());

        return movieMapper.toDetails(movieRepository.findByIdOrThrow(savedMovie.getId()));
    }
}