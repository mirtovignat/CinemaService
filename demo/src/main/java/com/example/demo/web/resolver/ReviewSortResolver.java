package com.example.demo.web.resolver;

import com.example.demo.cursor.SortDirection;
import com.example.demo.dto.catalog.ReviewsQueryDTO;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ReviewSortResolver {

    public static final String SORT_COOKIE_NAME = "reviewsSort";

    private final int sortCookieMaxAge;

    public ReviewSortResolver(@Value("${app.reviews.sort-cookie-max-age:31536000}")
                              int sortCookieMaxAge) {
        this.sortCookieMaxAge = sortCookieMaxAge;
    }

    public SortDirection resolveAndPersist(ReviewsQueryDTO reviewsQueryDTO,
                                           String sortCookie,
                                           HttpServletResponse httpServletResponse) {
        SortDirection resolvedSortDirection = resolve(reviewsQueryDTO, sortCookie);

        if (isExplicit(reviewsQueryDTO.sort()) || isExplicit(reviewsQueryDTO.sortDirection())) {
            writeCookie(httpServletResponse, resolvedSortDirection);
        }
        return resolvedSortDirection;
    }

    public SortDirection resolve(ReviewsQueryDTO reviewsQueryDTO, String sortCookie) {
        String rawSortDirection = pickNonBlank(
                reviewsQueryDTO.sort(),
                reviewsQueryDTO.sortDirection()
        );
        if (rawSortDirection != null) {
            return SortDirection.from(rawSortDirection);
        }
        SortDirection cookieSortDirection = parseCookie(sortCookie);
        return Objects.requireNonNullElse(cookieSortDirection, SortDirection.DESC);
    }

    private static String pickNonBlank(String sort, String sortDirection) {
        if (isExplicit(sort)) return sort;
        if (isExplicit(sortDirection)) return sortDirection;
        return null;
    }

    private static boolean isExplicit(String sortDirection) {
        return sortDirection != null && !sortDirection.isBlank();
    }

    private SortDirection parseCookie(String sortCookie) {
        if (sortCookie == null || sortCookie.isBlank()) {
            return null;
        }
        try {
            return SortDirection.valueOf(sortCookie.trim().toUpperCase());
        } catch (IllegalArgumentException illegalArgumentException) {
            return null;
        }
    }

    private void writeCookie(HttpServletResponse httpServletResponse,
                             SortDirection sortDirection) {
        Cookie sortCookie = new Cookie(SORT_COOKIE_NAME, sortDirection.lowercase());
        sortCookie.setPath("/rated");
        sortCookie.setMaxAge(sortCookieMaxAge);
        httpServletResponse.addCookie(sortCookie);
    }
}