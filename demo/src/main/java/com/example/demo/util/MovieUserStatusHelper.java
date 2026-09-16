package com.example.demo.util;

import com.example.demo.dto.base.UserStatuses;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.dto.movie.MovieUserStatus;
import com.example.demo.model.entity.CartItem;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.model.entity.Movie;
import com.example.demo.service.linked_collection.AbstractLinkedCollectionService;
import com.example.demo.service.purchased.PurchasedQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class MovieUserStatusHelper {

    private final PurchasedQueryService purchasedService;
    private final AbstractLinkedCollectionService<FavouriteMovie, FavouriteMovieViewDTO> favouritesService;
    private final AbstractLinkedCollectionService<CartItem, CartItemViewDTO> cartService;

    private UserStatuses getUserStatuses(Long userId) {
        return UserStatuses.builder()
                .bought(this.safeSet(this.purchasedService.getPurchasedMovieIds(userId)))
                .cart(this.safeSet(this.cartService.getMovieIds(userId)))
                .favourites(this.safeSet(this.favouritesService.getMovieIds(userId)))
                .build();
    }

    public Page<MovieForUser<MovieCardViewDTO>> enrichWithUserStatuses(
            Page<Movie> moviePage,
            Long userId,
            Function<Movie, MovieCardViewDTO> toCardMapper) {
        UserStatuses statuses = this.getUserStatuses(userId);
        return moviePage.map(movie -> {
            boolean isBought = statuses.bought().contains(movie.getId());
            boolean isInCart = (!isBought) && statuses.cart().contains(movie.getId());
            boolean isInFavourites = statuses.favourites().contains(movie.getId());
            return MovieForUser.<MovieCardViewDTO>builder()
                    .movie(toCardMapper.apply(movie))
                    .status(MovieUserStatus.builder()
                            .bought(isBought)
                            .inCart(isInCart)
                            .inFavourites(isInFavourites)
                            .build())
                    .build();
        });
    }

    private Set<Long> safeSet(Set<Long> set) {
        return set == null ? Set.of() : set;
    }
}