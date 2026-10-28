package com.example.demo.service.movie.poster;

import com.example.demo.config.TMDBConfig;
import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.dto.movie.tmdb.TMDBSearchResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class TMDBService {

    private final TMDBConfig tmdbConfig;
    private final RestClient restClient;

    public TMDBService(TMDBConfig tmdbConfig,
                       @Qualifier("jsonRestClient") RestClient restClient) {
        this.tmdbConfig = tmdbConfig;
        this.restClient = restClient;
    }

    public TMDBMovieDTO getMovieById(Long tmdbId) {
        URI uri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/movie/" + tmdbId)
                .queryParam("language", "ru-RU")
                .queryParam("append_to_response", "credits")
                .build()
                .encode()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(TMDBMovieDTO.class);
        } catch (HttpClientErrorException.NotFound notFound) {
            log.warn("TMDB movie not found: tmdbId={}", tmdbId);
            return null;
        } catch (HttpClientErrorException clientError) {
            log.warn("TMDB client error for tmdbId={}: {} ({})",
                    tmdbId, clientError.getStatusCode(), clientError.getMessage());
            return null;
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, uri);
        }
    }

    public List<TMDBMovieDTO> searchMovies(String query) {
        URI uri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/search/movie")
                .queryParam("language", "ru-RU")
                .queryParam("query", query)
                .queryParam("page", 1)
                .build()
                .encode()
                .toUri();

        TMDBSearchResponse response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(TMDBSearchResponse.class);
        } catch (HttpClientErrorException clientError) {
            log.warn("TMDB search client error for query='{}': {} ({})",
                    query, clientError.getStatusCode(), clientError.getMessage());
            return List.of();
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, uri);
        }

        return response != null && response.results() != null
                ? response.results()
                : List.of();
    }

    public List<TMDBMovieDTO> searchMoviesByDirector(String directorName) {
        Long directorId = findPersonId(directorName, "Directing");
        if (directorId == null) {
            return List.of();
        }
        return fetchMovieCredits(directorId, "Director");
    }

    public List<TMDBMovieDTO> searchMoviesByActor(String actorName) {
        Long actorId = findPersonId(actorName, "Acting");
        if (actorId == null) {
            return List.of();
        }
        return fetchMovieCredits(actorId, "Acting");
    }

    private Long findPersonId(String personName, String expectedDepartment) {
        URI uri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/search/person")
                .queryParam("language", "ru-RU")
                .queryParam("query", personName)
                .queryParam("page", 1)
                .build()
                .encode()
                .toUri();

        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException clientError) {
            log.warn("TMDB person search client error for '{}': {} ({})",
                    personName, clientError.getStatusCode(), clientError.getMessage());
            return null;
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, uri);
        }

        if (response == null || !response.has("results")) {
            return null;
        }

        JsonNode results = response.get("results");
        if (!results.isArray() || results.isEmpty()) {
            return null;
        }

        JsonNode fallback = results.get(0);
        for (JsonNode person : results) {
            String department = person.path("known_for_department").asText("");
            if (expectedDepartment.equalsIgnoreCase(department) && person.hasNonNull("id")) {
                return person.get("id").asLong();
            }
        }

        if (fallback.hasNonNull("id")) {
            return fallback.get("id").asLong();
        }
        return null;
    }

    private List<TMDBMovieDTO> fetchMovieCredits(Long personId, String role) {
        URI uri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/person/" + personId + "/movie_credits")
                .queryParam("language", "ru-RU")
                .build()
                .encode()
                .toUri();

        JsonNode response;
        try {
            response = restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException clientError) {
            log.warn("TMDB credits client error for personId={}: {} ({})",
                    personId, clientError.getStatusCode(), clientError.getMessage());
            return List.of();
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, uri);
        }

        if (response == null) {
            return List.of();
        }

        List<TMDBMovieDTO> movies = new ArrayList<>();

        if ("Director".equalsIgnoreCase(role) && response.has("crew")) {
            for (JsonNode entry : response.get("crew")) {
                if (!"Director".equalsIgnoreCase(entry.path("job").asText(""))) {
                    continue;
                }
                movies.add(mapCreditsEntry(entry));
            }
        } else if ("Acting".equalsIgnoreCase(role) && response.has("cast")) {
            for (JsonNode entry : response.get("cast")) {
                movies.add(mapCreditsEntry(entry));
            }
        }

        return movies;
    }

    private TMDBMovieDTO mapCreditsEntry(JsonNode entry) {
        return TMDBMovieDTO.builder()
                .id(entry.path("id").asLong())
                .title(entry.path("title").asText(null))
                .originalTitle(entry.path("original_title").asText(null))
                .releaseDate(entry.path("release_date").asText(null))
                .posterPath(entry.path("poster_path").asText(null))
                .build();
    }

    public String getPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) return null;
        return tmdbConfig.getImageUrl() + "/" + tmdbConfig.getPosterSize() + posterPath;
    }

    private BusinessException tmdbUnavailable(ResourceAccessException exception, URI uri) {
        log.warn("TMDB request failed: {} ({})", uri, exception.getMessage());
        return BusinessException.of(ErrorCode.TMDB_UNAVAILABLE);
    }
}