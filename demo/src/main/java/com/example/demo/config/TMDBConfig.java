package com.example.demo.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class TMDBConfig {

    @Value("${tmdb.api-url}")
    private String apiUrl;

    @Value("${tmdb.image-url}")
    private String imageUrl;

    @Value("${tmdb.access-token}")
    private String accessToken;

    @Value("${tmdb.poster-size:w500}")
    private String posterSize;
}