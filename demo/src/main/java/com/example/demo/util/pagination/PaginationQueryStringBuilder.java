package com.example.demo.util.pagination;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class PaginationQueryStringBuilder {

    private static final String[] SINGLE_PARAMS = {
            "title", "genre",
            "priceFrom", "priceTo",
            "ratingFrom", "ratingTo",
            "durationFrom", "durationTo",
            "releaseDateFrom", "releaseDateTo",
            "sortBy", "getSortDirection"
    };

    private static final String[] ARRAY_PARAMS = {
            "genres", "genresExclude",
            "directors", "directorsExclude",
            "actors"
    };

    private static final String[] FLAG_PARAMS = {
            "directorsAll", "genresAll"
    };

    public String build() {
        HttpServletRequest httpServletRequest = currentRequest();
        StringBuilder stringBuilder = new StringBuilder();

        for (String name : SINGLE_PARAMS) {
            String value = httpServletRequest.getParameter(name);
            if (value != null && !value.isEmpty()) {
                stringBuilder.append('&').append(name).append('=').append(urlEncode(value));
            }
        }

        for (String name : ARRAY_PARAMS) {
            String[] values = httpServletRequest.getParameterValues(name);
            if (values != null && values.length > 0) {
                stringBuilder.append('&').append(name).append('=').append(urlEncode(join(values)));
            }
        }

        for (String name : FLAG_PARAMS) {
            String value = httpServletRequest.getParameter(name);
            if ("true".equalsIgnoreCase(value)) {
                stringBuilder.append('&').append(name).append('=').append("true");
            }
        }

        return stringBuilder.toString();
    }

    private static HttpServletRequest currentRequest() {
        return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                .getRequest();
    }

    private static String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static String join(String[] values) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) {
                stringBuilder.append(',');
            }
            stringBuilder.append(values[i]);
        }
        return stringBuilder.toString();
    }
}