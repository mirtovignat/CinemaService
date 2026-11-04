package com.example.demo.dto.movie;

import com.example.demo.dto.base.UserStatus;
import lombok.Builder;

@Builder
public record MovieUserStatus(
        boolean bought,
        boolean inCart,
        boolean inFavourites
) {
    public static final MovieUserStatus EMPTY = MovieUserStatus.builder().build();

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

    public static MovieUserStatus from(UserStatus userStatus) {
        boolean bought = userStatus.bought();
        boolean inCart = !bought && userStatus.inCart();
        return MovieUserStatus.builder()
                .bought(bought)
                .inCart(inCart)
                .inFavourites(userStatus.inFavourites())
                .build();
    }
}