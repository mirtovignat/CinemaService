package com.example.demo.util;

import com.example.demo.dto.base.UserStatuses;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieForUser;
import com.example.demo.dto.movie.MovieUserStatus;
import com.example.demo.model.entity.Movie;
import com.example.demo.service.linked_collection.UserStatusResolver;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.function.Function;

@Component
@AllArgsConstructor
public class MovieUserStatusHelper {

    private final UserStatusResolver userStatusResolver;

    public Page<MovieForUser<MovieCardViewDTO>> enrichWithUserStatuses(
            Page<Movie> moviePage,
            Long userId,
            Function<Movie, MovieCardViewDTO> movieCardMapper) {
        UserStatuses userStatuses = userStatusResolver.resolveUserStatuses(userId);
        return moviePage.map(movie -> buildMovieForUser(movie, movieCardMapper, userStatuses));
    }

    private MovieForUser<MovieCardViewDTO> buildMovieForUser(
            Movie movie,
            Function<Movie, MovieCardViewDTO> movieCardMapper,
            UserStatuses userStatuses) {
        boolean isBought = userStatuses.bought().contains(movie.getId());
        boolean isInCart = !isBought && userStatuses.cart().contains(movie.getId());
        boolean isInFavourites = userStatuses.favourites().contains(movie.getId());
        ReviewViewDTO ownRating = userStatuses.rated().get(movie.getId());
        return MovieForUser.<MovieCardViewDTO>builder()
                .movie(movieCardMapper.apply(movie))
                .status(MovieUserStatus.builder()
                        .bought(isBought)
                        .inCart(isInCart)
                        .inFavourites(isInFavourites)
                        .ownRating(ownRating)
                        .build())
                .build();
    }
}