package com.example.demo.service.movie;

import java.util.List;

@FunctionalInterface
public interface KeysetFetcher {
    List<String> fetch(
            String normalizedQuery,
            boolean hasCursor,
            String cursor,
            int limit
    );
}