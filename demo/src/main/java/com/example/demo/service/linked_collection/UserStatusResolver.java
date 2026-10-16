package com.example.demo.service.linked_collection;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.base.UserStatuses;
import com.example.demo.repository.entity.CartItemRepository;
import com.example.demo.repository.entity.FavouriteMovieRepository;
import com.example.demo.repository.entity.PurchasedMovieRepository;
import com.example.demo.service.rated.RatedQueryService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@AllArgsConstructor
public class UserStatusResolver {

    private final CartItemRepository cartItemRepository;
    private final FavouriteMovieRepository favouriteMovieRepository;
    private final PurchasedMovieRepository purchasedMovieRepository;
    private final RatedQueryService ratedQueryService;

    public Map<Long, UserStatus> resolveAll(Long userId, Collection<Long> movieIds) {
        if (userId == null || movieIds == null || movieIds.isEmpty()) {
            return Map.of();
        }

        Set<Long> cartMovieIds = new LinkedHashSet<>(
                cartItemRepository.findMovieIdsByUserIdAndMovieIdIn(userId, movieIds));
        Set<Long> favouriteMovieIds = new LinkedHashSet<>(
                favouriteMovieRepository.findMovieIdsByUserIdAndMovieIdIn(userId, movieIds));
        Set<Long> purchasedMovieIds = new LinkedHashSet<>(
                purchasedMovieRepository.findMovieIdsByUserIdAndMovieIdIn(userId, movieIds));

        Map<Long, UserStatus> userStatuses = new LinkedHashMap<>(movieIds.size());
        for (Long movieId : movieIds) {
            userStatuses.put(movieId, UserStatus.builder()
                    .inCart(cartMovieIds.contains(movieId))
                    .inFavourites(favouriteMovieIds.contains(movieId))
                    .bought(purchasedMovieIds.contains(movieId))
                    .build());
        }
        return userStatuses;
    }

    public UserStatuses resolveUserStatuses(Long userId) {
        if (userId == null) {
            return UserStatuses.empty();
        }
        return UserStatuses.builder()
                .cart(new LinkedHashSet<>(cartItemRepository.findMovieIdsByUserId(userId)))
                .favourites(new LinkedHashSet<>(favouriteMovieRepository.findMovieIdsByUserId(userId)))
                .bought(new LinkedHashSet<>(purchasedMovieRepository.findMovieIdsByUserId(userId)))
                .rated(new LinkedHashMap<>(ratedQueryService.getOwnRatings(userId)))
                .build();
    }
}