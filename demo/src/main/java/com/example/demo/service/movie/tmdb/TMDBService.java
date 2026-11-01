package com.example.demo.service.movie.tmdb;

import com.example.demo.config.TMDBConfig;
import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.dto.movie.tmdb.search.TMDBSearchResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
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

@Service
public class TMDBService {

    private static final long REQUEST_DELAY_MS = 150;

    private final TMDBConfig tmdbConfig;
    private final RestClient restClient;

    public TMDBService(TMDBConfig tmdbConfig,
                       @Qualifier("jsonRestClient") RestClient restClient) {
        this.tmdbConfig = tmdbConfig;
        this.restClient = restClient;
    }

    public TMDBMovieDTO getMovieById(Long tmdbId) {
        TMDBMovieDTO movie = fetchMovieById(tmdbId, "ru-RU");
        if (movie == null) {
            return null;
        }
        if (movie.title() == null || movie.title().isBlank()) {
            return null;
        }
        if (movie.overview() == null || movie.overview().isBlank()) {
            return null;
        }
        return movie;
    }

    public String getCertification(Long tmdbId) {
        sleepForTmdb();
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
            return null;
        }
    }

    public String extractCertification(TMDBMovieDTO tmdbMovieDTO) {
        if (tmdbMovieDTO == null || tmdbMovieDTO.id() == null) {
            return null;
        }
        return getCertification(tmdbMovieDTO.id());
    }

    private TMDBMovieDTO fetchMovieById(Long tmdbId, String languageCode) {
        sleepForTmdb();
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
            return null;
        } catch (HttpClientErrorException clientErrorException) {
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
            }
        }

        return allResults;
    }

    private List<TMDBMovieDTO> fetchSearchMovies(String searchQuery, String languageCode, int page) {
        sleepForTmdb();
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
        sleepForTmdb();
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
        sleepForTmdb();
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

    public void pauseBetweenRequests() {
        sleepForTmdb();
    }

    private void sleepForTmdb() {
        try {
            Thread.sleep(REQUEST_DELAY_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private BusinessException tmdbUnavailable(ResourceAccessException resourceAccessException, URI requestUri) {
        return BusinessException.of(ErrorCode.TMDB_UNAVAILABLE);
    }
}