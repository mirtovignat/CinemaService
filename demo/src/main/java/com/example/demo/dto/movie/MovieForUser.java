package com.example.demo.dto.movie;

import lombok.Builder;

@Builder
public record MovieForUser<Movie>(
        Movie movie,
        MovieUserStatus status
) {
}