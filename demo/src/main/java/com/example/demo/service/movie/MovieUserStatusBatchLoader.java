package com.example.demo.service.movie;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.dto.movie.MovieUserStatus;
import com.example.demo.service.catalog.RatedQueryService;
import com.example.demo.service.linked_collection.UserStatusResolver;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@AllArgsConstructor
public class MovieUserStatusBatchLoader {

    private final UserStatusResolver userStatusResolver;
    private final RatedQueryService ratedQueryService;

    @Transactional(readOnly = true)
    public Map<Long, MovieUserStatus> loadForMovies(List<Long> movieIds, Long userId) {
        if (movieIds == null || movieIds.isEmpty() || userId == null) {
            return Map.of();
        }

        Map<Long, UserStatus> statuses = userStatusResolver.loadAllUserStatuses(userId, movieIds);
        Map<Long, ReviewViewDTO> ownRatings = ratedQueryService.getOwnRatingsForMovies(movieIds, userId);

        Map<Long, MovieUserStatus> result = new HashMap<>(movieIds.size());
        for (Long movieId : movieIds) {
            UserStatus userStatus = statuses.getOrDefault(movieId, UserStatus.builder().build());
            boolean isBought = userStatus.bought();
            boolean isInCart = !isBought && userStatus.inCart();
            result.put(
                    movieId,
                    MovieUserStatus.builder()
                            .bought(isBought)
                            .inCart(isInCart)
                            .inFavourites(userStatus.inFavourites())
                            .ownRating(ownRatings.get(movieId))
                            .build()
            );
        }
        return result;
    }
}