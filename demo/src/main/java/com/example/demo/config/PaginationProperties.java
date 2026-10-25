package com.example.demo.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.pagination")
public class PaginationProperties {
    private int defaultSize = 35;
    private int genreDefaultSize = 15;
    private int maxSize = 100;
}