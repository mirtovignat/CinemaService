package com.example.demo.dto.catalog;

import com.example.demo.dto.base.*;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record LibrarianMovieViewDTO(
        boolean watched,
        LocalDateTime addedAt,
        Long historyItemId,
        @JsonUnwrapped MovieIdentity id,
        @JsonUnwrapped MovieTitled title,
        @JsonUnwrapped MovieMedia media,
        @JsonUnwrapped MovieRating rating,
        @JsonUnwrapped MovieRelease release,
        @JsonUnwrapped MovieMeta meta
) {
}