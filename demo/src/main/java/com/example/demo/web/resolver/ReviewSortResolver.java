package com.example.demo.web.resolver;

import com.example.demo.cursor.SortDirection;
import com.example.demo.dto.catalog.ReviewsQueryDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ReviewSortResolver {

    public static final String SORT_COOKIE_NAME = "reviewsSort";

    private final int sortCookieMaxAge;

    public ReviewSortResolver(@Value("${app.reviews.sort-cookie-max-age:31536000}") int sortCookieMaxAge) {
        this.sortCookieMaxAge = sortCookieMaxAge;
    }

    public SortDirection resolveAndPersist(ReviewsQueryDTO reviewsQueryDTO,
                                           String cookieValue,
                                           HttpServletResponse httpServletResponse) {
        SortDirection sortDirection = resolve(reviewsQueryDTO, cookieValue);

        if (isExplicit(reviewsQueryDTO.sort())) {
            writeCookie(httpServletResponse, sortDirection);
        }
        return sortDirection;
    }

    public SortDirection resolve(ReviewsQueryDTO reviewsQueryDTO, String cookieValue) {
        if (reviewsQueryDTO.sort() != null) {
            return reviewsQueryDTO.sort();
        }
        SortDirection fromCookie = parseCookie(cookieValue);
        if (fromCookie != null) {
            return fromCookie;
        }
        return SortDirection.DESC;
    }

    private boolean isExplicit(SortDirection sort) {
        return sort != null;
    }

    private SortDirection parseCookie(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return SortDirection.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private void writeCookie(HttpServletResponse httpServletResponse,
                             SortDirection sortDirection) {
        Cookie cookie = new Cookie(SORT_COOKIE_NAME, sortDirection.lowercase());
        cookie.setPath("/rated");
        cookie.setMaxAge(sortCookieMaxAge);
        httpServletResponse.addCookie(cookie);
    }
}