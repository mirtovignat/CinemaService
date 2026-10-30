package com.example.demo.repository.projection;

import java.time.LocalDate;

public interface MovieReferProjection {
    Long getId();
    String getTitle();
    LocalDate getReleaseDate();
    Long getTmdbId();
}