package com.example.demo.dto.movie;

import lombok.Builder;

@Builder
public record MovieUserStatus(
        boolean bought,
        boolean inCart,
        boolean inFavourites
) {
    public static MovieUserStatus of(
            boolean bought,
            boolean inCart,
            boolean inFavourites) {
        return MovieUserStatus.builder()
                .bought(bought)
                .inCart(inCart)
                .inFavourites(inFavourites)
                .build();
    }
}