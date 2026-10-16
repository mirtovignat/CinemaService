package com.example.demo.web.resolver;

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

    public ReviewsQueryDTO.SortDirection resolveAndPersist(ReviewsQueryDTO reviewsQueryDTO,
                                                           String cookieValue,
                                                           HttpServletResponse httpServletResponse) {
        ReviewsQueryDTO.SortDirection sortDirection = resolve(reviewsQueryDTO.sort(), cookieValue);

        if (isExplicit(reviewsQueryDTO.sort())) {
            writeCookie(httpServletResponse, sortDirection);
        }
        return sortDirection;
    }

    public ReviewsQueryDTO.SortDirection resolve(ReviewsQueryDTO reviewsQueryDTO, String cookieValue) {
        return resolve(reviewsQueryDTO.sort(), cookieValue);
    }

    private ReviewsQueryDTO.SortDirection resolve(String rawSort, String cookieValue) {
        if (isExplicit(rawSort)) {
            return "ASC".equalsIgnoreCase(rawSort)
                    ? ReviewsQueryDTO.SortDirection.ASC
                    : ReviewsQueryDTO.SortDirection.DESC;
        }
        if ("ASC".equalsIgnoreCase(cookieValue)) {
            return ReviewsQueryDTO.SortDirection.ASC;
        }
        return ReviewsQueryDTO.SortDirection.DESC;
    }

    private boolean isExplicit(String rawSort) {
        return rawSort != null && !rawSort.isBlank();
    }

    private void writeCookie(HttpServletResponse httpServletResponse,
                             ReviewsQueryDTO.SortDirection sortDirection) {
        Cookie cookie = new Cookie(SORT_COOKIE_NAME, sortDirection.name());
        cookie.setPath("/rated");
        cookie.setMaxAge(sortCookieMaxAge);
        httpServletResponse.addCookie(cookie);
    }
}