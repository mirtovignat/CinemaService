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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
        TMDBMovieDTO russianMovie = fetchMovieById(tmdbId, "ru-RU");
        if (russianMovie == null || russianMovie.title() == null || russianMovie.title().isBlank()) {
            TMDBMovieDTO englishMovie = fetchMovieById(tmdbId, "en-US");
            if (englishMovie != null) {
                return englishMovie;
            }
        }
        return russianMovie;
    }

    public String getCertification(Long tmdbId) {
        URI requestUri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/movie/" + tmdbId + "/release_dates")
                .build()
                .encode()
                .toUri();
        try {
            JsonNode root = restClient.get()
                    .uri(requestUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
            if (root == null || !root.has("results")) return null;
            for (JsonNode country : root.get("results")) {
                String iso = country.path("iso_3166_1").asText("");
                if ("RU".equalsIgnoreCase(iso) || "US".equalsIgnoreCase(iso)) {
                    JsonNode dates = country.path("release_dates");
                    if (dates.isArray()) {
                        for (JsonNode d : dates) {
                            String cert = d.path("certification").asText("");
                            if (!cert.isBlank()) return cert;
                        }
                    }
                }
            }
            return null;
        } catch (Exception e) {
            log.warn("Failed to fetch certification for tmdbId={}: {}", tmdbId, e.getMessage());
            return null;
        }
    }

    private TMDBMovieDTO fetchMovieById(Long tmdbId, String languageCode) {
        URI requestUri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/movie/" + tmdbId)
                .queryParam("language", languageCode)
                .queryParam("append_to_response", "credits")
                .build()
                .encode()
                .toUri();

        try {
            return restClient.get()
                    .uri(requestUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(TMDBMovieDTO.class);
        } catch (HttpClientErrorException.NotFound notFoundException) {
            log.warn("TMDB movie not found: tmdbId={}", tmdbId);
            return null;
        } catch (HttpClientErrorException clientErrorException) {
            log.warn("TMDB client error for tmdbId={}: {} ({})",
                    tmdbId, clientErrorException.getStatusCode(), clientErrorException.getMessage());
            return null;
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, requestUri);
        }
    }

    public List<TMDBMovieDTO> searchMovies(String searchQuery) {
        List<TMDBMovieDTO> allResults = new ArrayList<>();
        Set<Long> seenIds = new LinkedHashSet<>();

        for (int page = 1; page <= 3; page++) {
            List<TMDBMovieDTO> pageResults = fetchSearchMovies(searchQuery, "ru-RU", page);
            for (TMDBMovieDTO dto : pageResults) {
                if (dto == null || dto.id() == null) continue;
                if (seenIds.add(dto.id())) {
                    allResults.add(dto);
                }
            }
            if (pageResults.size() < 20) break;
            try {
                Thread.sleep(120);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }

        if (allResults.isEmpty()) {
            for (int page = 1; page <= 2; page++) {
                List<TMDBMovieDTO> pageResults = fetchSearchMovies(searchQuery, "en-US", page);
                for (TMDBMovieDTO dto : pageResults) {
                    if (dto == null || dto.id() == null) continue;
                    if (seenIds.add(dto.id())) {
                        allResults.add(dto);
                    }
                }
                if (pageResults.size() < 20) break;
                try {
                    Thread.sleep(120);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        return allResults;
    }

    private List<TMDBMovieDTO> fetchSearchMovies(String searchQuery, String languageCode, int page) {
        URI requestUri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/search/movie")
                .queryParam("language", languageCode)
                .queryParam("query", searchQuery)
                .queryParam("page", page)
                .build()
                .encode()
                .toUri();

        TMDBSearchResponse searchResponse;
        try {
            searchResponse = restClient.get()
                    .uri(requestUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(TMDBSearchResponse.class);
        } catch (HttpClientErrorException clientErrorException) {
            log.warn("TMDB search client error for query='{}' page={}: {} ({})",
                    searchQuery, page, clientErrorException.getStatusCode(), clientErrorException.getMessage());
            return List.of();
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, requestUri);
        }

        return searchResponse != null && searchResponse.results() != null
                ? searchResponse.results()
                : List.of();
    }

    public List<TMDBMovieDTO> searchMoviesByDirector(String directorName) {
        Long directorPersonId = findPersonId(directorName, "Directing");
        if (directorPersonId == null) {
            return List.of();
        }
        return fetchMovieCredits(directorPersonId, "Director");
    }

    public List<TMDBMovieDTO> searchMoviesByActor(String actorName) {
        Long actorPersonId = findPersonId(actorName, "Acting");
        if (actorPersonId == null) {
            return List.of();
        }
        return fetchMovieCredits(actorPersonId, "Acting");
    }

    private Long findPersonId(String personName, String expectedDepartment) {
        URI requestUri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/search/person")
                .queryParam("language", "ru-RU")
                .queryParam("query", personName)
                .queryParam("page", 1)
                .build()
                .encode()
                .toUri();

        JsonNode responseNode;
        try {
            responseNode = restClient.get()
                    .uri(requestUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException clientErrorException) {
            log.warn("TMDB person search client error for '{}': {} ({})",
                    personName, clientErrorException.getStatusCode(), clientErrorException.getMessage());
            return null;
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, requestUri);
        }

        if (responseNode == null || !responseNode.has("results")) {
            return null;
        }

        JsonNode resultsNode = responseNode.get("results");
        if (!resultsNode.isArray() || resultsNode.isEmpty()) {
            return null;
        }

        JsonNode fallbackPersonNode = resultsNode.get(0);
        for (JsonNode personNode : resultsNode) {
            String knownForDepartment = personNode.path("known_for_department").asText("");
            if (expectedDepartment.equalsIgnoreCase(knownForDepartment) && personNode.hasNonNull("id")) {
                return personNode.get("id").asLong();
            }
        }

        if (fallbackPersonNode.hasNonNull("id")) {
            return fallbackPersonNode.get("id").asLong();
        }
        return null;
    }

    private List<TMDBMovieDTO> fetchMovieCredits(Long personId, String roleName) {
        URI requestUri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/person/" + personId + "/movie_credits")
                .queryParam("language", "ru-RU")
                .build()
                .encode()
                .toUri();

        JsonNode responseNode;
        try {
            responseNode = restClient.get()
                    .uri(requestUri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(JsonNode.class);
        } catch (HttpClientErrorException clientErrorException) {
            log.warn("TMDB credits client error for personId={}: {} ({})",
                    personId, clientErrorException.getStatusCode(), clientErrorException.getMessage());
            return List.of();
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, requestUri);
        }

        if (responseNode == null) {
            return List.of();
        }

        List<TMDBMovieDTO> movieList = new ArrayList<>();

        if ("Director".equalsIgnoreCase(roleName) && responseNode.has("crew")) {
            for (JsonNode crewEntryNode : responseNode.get("crew")) {
                if (!"Director".equalsIgnoreCase(crewEntryNode.path("job").asText(""))) {
                    continue;
                }
                movieList.add(mapCreditsEntry(crewEntryNode));
            }
        } else if ("Acting".equalsIgnoreCase(roleName) && responseNode.has("cast")) {
            for (JsonNode castEntryNode : responseNode.get("cast")) {
                movieList.add(mapCreditsEntry(castEntryNode));
            }
        }

        return movieList;
    }

    private TMDBMovieDTO mapCreditsEntry(JsonNode creditsEntryNode) {
        return TMDBMovieDTO.builder()
                .id(creditsEntryNode.path("id").asLong())
                .title(creditsEntryNode.path("title").asText(null))
                .originalTitle(creditsEntryNode.path("original_title").asText(null))
                .releaseDate(creditsEntryNode.path("release_date").asText(null))
                .posterPath(creditsEntryNode.path("poster_path").asText(null))
                .build();
    }

    public String getPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) {
            return null;
        }
        return tmdbConfig.getImageUrl() + "/" + tmdbConfig.getPosterSize() + posterPath;
    }

    private BusinessException tmdbUnavailable(ResourceAccessException resourceAccessException, URI requestUri) {
        log.warn("TMDB request failed: {} ({})", requestUri, resourceAccessException.getMessage());
        return BusinessException.of(ErrorCode.TMDB_UNAVAILABLE);
    }
}