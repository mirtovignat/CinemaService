package com.example.demo.filter;

import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class MovieFilterQueryBuilder {

    public String build(MovieFilter movieFilter, MovieSearchContext movieSearchContext) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();

        addIfPresent(params, "title", movieSearchContext.title());
        addIfPresent(params, "sortBy", movieSearchContext.sortBy());
        addIfPresent(params, "sortDirection", movieSearchContext.sortDirection());
        addIfPresent(params, "genre", movieSearchContext.genre());

        if (movieFilter != null) {
            addBigDecimal(params, "priceFrom", movieFilter.priceFrom());
            addBigDecimal(params, "priceTo", movieFilter.priceTo());
            addBigDecimal(params, "ratingFrom", movieFilter.ratingFrom());
            addBigDecimal(params, "ratingTo", movieFilter.ratingTo());
            addDate(params, "releaseDateFrom", movieFilter.releaseDateFrom());
            addDate(params, "releaseDateTo", movieFilter.releaseDateTo());
            addInt(params, "durationFrom", movieFilter.durationFrom());
            addInt(params, "durationTo", movieFilter.durationTo());
            addList(params, "genres", movieFilter.genres());
            addList(params, "directors", movieFilter.directors());
        }

        if (params.isEmpty()) {
            return "";
        }

        return "&" + params.entrySet().stream()
                .flatMap(element -> element.getValue().stream()
                        .map(value -> element.getKey() + "=" + encode(value)))
                .collect(Collectors.joining("&"));
    }

    private void addIfPresent(MultiValueMap<String, String> params, String key, String value) {
        if (value != null && !value.isBlank()) {
            params.add(key, value);
        }
    }

    private void addBigDecimal(MultiValueMap<String, String> params, String key, BigDecimal value) {
        if (value != null) {
            params.add(key, value.toPlainString());
        }
    }

    private void addDate(MultiValueMap<String, String> params, String key, LocalDate value) {
        if (value != null) {
            params.add(key, value.toString());
        }
    }

    private void addInt(MultiValueMap<String, String> params, String key, Integer value) {
        if (value != null) {
            params.add(key, value.toString());
        }
    }

    private void addList(MultiValueMap<String, String> params, String key, List<String> values) {
        if (values == null) {
            return;
        }
        values.stream()
                .filter(value -> value != null && !value.isBlank())
                .forEach(value -> params.add(key, value));
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}