package com.example.demo.service.cursor;

import org.springframework.data.domain.Pageable;

import java.util.List;

public interface SuggestableCursorService {

    List<String> findTitlesStartingWith(
            Long userId,
            String normalizedQuery,
            Pageable pageable
    );
}