package com.example.demo.mapper.entity;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.mapper.util.MovieComponentMapper;
import com.example.demo.model.entity.FavouriteMovie;
import com.example.demo.service.precence.FavouritePresenceService;
import com.example.demo.service.purchased.PurchasedQueryService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {MovieComponentMapper.class})
public abstract class FavouritesMapper {

    @Autowired
    protected PurchasedQueryService purchasedQueryService;

    @Mapping(target = "id", source = "movie", qualifiedByName = "toIdentity")
    @Mapping(target = "title", source = "movie", qualifiedByName = "toTitled")
    @Mapping(target = "price", source = "movie", qualifiedByName = "toPricing")
    @Mapping(target = "media", source = "movie", qualifiedByName = "toMedia")
    @Mapping(target = "rating", source = "movie", qualifiedByName = "toRating")
    @Mapping(target = "release", source = "movie", qualifiedByName = "toRelease")
    @Mapping(target = "meta", source = "movie", qualifiedByName = "toMetaWithoutDescription")
    @Mapping(target = "status", expression = "java(mapUserStatus(favouriteMovie, favouritePresenceService))")
    public abstract FavouriteMovieViewDTO toOwnerView(FavouriteMovie favouriteMovie,
                                                      @Context FavouritePresenceService favouritePresenceService);

    protected UserStatus mapUserStatus(FavouriteMovie favouriteMovie, FavouritePresenceService favouritePresenceService) {
        if (favouriteMovie == null || favouriteMovie.getMovie() == null || favouriteMovie.getUser() == null) {
            return UserStatus.builder()
                    .inCart(false)
                    .inFavourites(false)
                    .bought(false)
                    .build();
        }
        Long movieId = favouriteMovie.getMovie().getId();
        Long userId = favouriteMovie.getUser().getId();
        return UserStatus.builder()
                .inCart(favouritePresenceService.isInLinkedCollection(movieId, userId))
                .inFavourites(true)
                .bought(purchasedQueryService.isMoviePurchasedByUser(movieId, userId))
                .build();
    }
}