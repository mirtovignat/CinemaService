package com.example.demo.client;

import com.example.demo.config.TMDBConfig;
import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.dto.movie.tmdb.person.TMDBPersonCreditsResponse;
import com.example.demo.dto.movie.tmdb.person.TMDBPersonSearchResponse;
import com.example.demo.dto.movie.tmdb.search.TMDBSearchResponse;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@Component
public class TMDBClient {

    private static final String APPEND_MOVIE = "credits,release_dates";

    private final TMDBConfig tmdbConfig;
    private final RestClient restClient;

    public TMDBClient(TMDBConfig tmdbConfig,
                      @Qualifier("jsonRestClient") RestClient restClient) {
        this.tmdbConfig = tmdbConfig;
        this.restClient = restClient;
    }

    public TMDBMovieDTO fetchMovie(Long tmdbId, String language) {
        URI uri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/movie/" + tmdbId)
                .queryParam("language", language)
                .queryParam("append_to_response", APPEND_MOVIE)
                .build()
                .encode()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(TMDBMovieDTO.class);
        } catch (HttpClientErrorException httpClientErrorException) {
            return null;
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, uri);
        }
    }

    public TMDBSearchResponse searchMovies(String query, String language, int page) {
        URI uri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/search/movie")
                .queryParam("language", language)
                .queryParam("query", query)
                .queryParam("page", page)
                .build()
                .encode()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(TMDBSearchResponse.class);
        } catch (HttpClientErrorException httpClientErrorException) {
            return null;
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, uri);
        }
    }

    public TMDBPersonSearchResponse searchPerson(String query, String language, int page) {
        URI uri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/search/person")
                .queryParam("language", language)
                .queryParam("query", query)
                .queryParam("page", page)
                .build()
                .encode()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(TMDBPersonSearchResponse.class);
        } catch (HttpClientErrorException httpClientErrorException) {
            return null;
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, uri);
        }
    }

    public TMDBPersonCreditsResponse fetchPersonMovieCredits(Long personId, String language) {
        URI uri = UriComponentsBuilder
                .fromUriString(tmdbConfig.getApiUrl() + "/person/" + personId + "/movie_credits")
                .queryParam("language", language)
                .build()
                .encode()
                .toUri();

        try {
            return restClient.get()
                    .uri(uri)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + tmdbConfig.getAccessToken())
                    .retrieve()
                    .body(TMDBPersonCreditsResponse.class);
        } catch (HttpClientErrorException httpClientErrorException) {
            return null;
        } catch (ResourceAccessException resourceAccessException) {
            throw tmdbUnavailable(resourceAccessException, uri);
        }
    }

    private BusinessException tmdbUnavailable(ResourceAccessException resourceAccessException, URI uri) {
        return BusinessException.of(ErrorCode.TMDB_UNAVAILABLE);
    }

}