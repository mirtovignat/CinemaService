package com.example.demo.service.movie.poster;

import com.example.demo.config.TMDBConfig;
import com.example.demo.dto.movie.TMDBMovieDTO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

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
                .queryParam("language", "en-US")
                .build()
                .encode()
                .toUri();

        return restClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + tmdbConfig.getAccessToken())
                .retrieve()
                .body(TMDBMovieDTO.class);
    }

    public String getPosterUrl(String posterPath) {
        if (posterPath == null || posterPath.isBlank()) return null;
        return tmdbConfig.getImageUrl() + "/" + tmdbConfig.getPosterSize() + posterPath;
    }
}