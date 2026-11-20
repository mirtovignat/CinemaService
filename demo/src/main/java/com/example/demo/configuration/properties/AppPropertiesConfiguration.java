package com.example.demo.configuration.properties;

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
        FiltersProperties.class,
        SecurityProperties.class,
        ValidationProperties.class
})
public class AppPropertiesConfiguration {
}