package com.example.demo.config.properties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        WalletProperties.class,
        PaginationProperties.class,
        SuggestionsProperties.class,
        RatingProperties.class,
        ReviewsProperties.class,
        CatalogProperties.class,
        RecommendationsProperties.class,
        FiltersProperties.class
})
public class AppPropertiesConfig {
}