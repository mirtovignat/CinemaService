package com.example.demo.dto.base;

import lombok.Builder;

@Builder
public record UserStatus(
        boolean inCart,
        boolean inFavourites,
        boolean bought
) {
}