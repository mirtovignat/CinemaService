package com.example.demo.dto;

import lombok.Builder;

@Builder
public record UserStatus(
        boolean inCart,
        boolean inFavourites,
        boolean bought
) {
    public static final UserStatus EMPTY = UserStatus.builder().build();
}