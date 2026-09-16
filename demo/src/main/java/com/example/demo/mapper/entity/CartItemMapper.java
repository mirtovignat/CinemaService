package com.example.demo.mapper.entity;

import com.example.demo.dto.base.CatalogQuantity;
import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.mapper.util.MovieComponentMapper;
import com.example.demo.model.entity.CartItem;
import com.example.demo.service.precence.CartPresenceService;
import com.example.demo.service.purchased.PurchasedQueryService;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {MovieComponentMapper.class})
public abstract class CartItemMapper {

    @Autowired
    protected PurchasedQueryService purchasedQueryService;

    @Mapping(target = "id", source = "movie", qualifiedByName = "toIdentity")
    @Mapping(target = "title", source = "movie", qualifiedByName = "toTitled")
    @Mapping(target = "price", source = "movie", qualifiedByName = "toPricing")
    @Mapping(target = "media", source = "movie", qualifiedByName = "toMedia")
    @Mapping(target = "rating", source = "movie", qualifiedByName = "toRating")
    @Mapping(target = "release", source = "movie", qualifiedByName = "toRelease")
    @Mapping(target = "meta", source = "movie", qualifiedByName = "toMetaWithoutDescription")
    @Mapping(target = "status", expression = "java(mapUserStatus(cartItem, cartPresenceService))")
    @Mapping(target = "quantity", expression = "java(mapToCatalogQuantity(cartItem.getQuantity()))")
    public abstract CartItemViewDTO toOwnerView(CartItem cartItem,
                                                @Context CartPresenceService cartPresenceService);

    protected CatalogQuantity mapToCatalogQuantity(Short quantity) {
        return quantity == null ? null : new CatalogQuantity(quantity);
    }

    protected UserStatus mapUserStatus(CartItem cartItem, CartPresenceService cartPresenceService) {
        if (cartItem == null || cartItem.getMovie() == null || cartItem.getUser() == null) {
            return UserStatus.builder()
                    .inCart(false)
                    .inFavourites(false)
                    .bought(false)
                    .build();
        }
        Long movieId = cartItem.getMovie().getId();
        Long userId = cartItem.getUser().getId();
        return UserStatus.builder()
                .inCart(true)
                .inFavourites(cartPresenceService.isInLinkedCollection(movieId, userId))
                .bought(purchasedQueryService.isMoviePurchasedByUser(movieId, userId))
                .build();
    }
}