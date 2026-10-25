package com.example.demo.repository.projection;

import java.time.LocalDateTime;

public interface WatchedSuggestionProjection {

    Long getMovieId();

    String getTitle();

    Long getWatchCount();

    LocalDateTime getLastWatchedAt();
}