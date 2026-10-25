package com.example.demo.filter;

import lombok.Builder;
import org.springframework.web.bind.annotation.BindParam;

@Builder
public record PageRequestContext(
        @BindParam("sortBy") String sortBy,
        @BindParam("sortDirection") String sortDirection,
        @BindParam("page") Integer page,
        @BindParam("size") Integer size
) {

    public static final int MAX_PAGE_SIZE = 100;

    public int getSafePage() {
        return page != null ? Math.max(page, 0) : 0;
    }

    public int getSafeSize(int defaultSize) {
        if (size == null || size <= 0) {
            return defaultSize;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}