package com.example.demo.mapper.entity;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.CartItemViewDTO;
import com.example.demo.mapper.util.DurationMapper;
import com.example.demo.model.entity.CartItem;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DurationMapper.class})
public interface CartItemMapper {

    @BeanMapping(ignoreByDefault = false)
    @Mapping(target = "id", source = "cartItem.movie.id")
    @Mapping(target = "title", source = "cartItem.movie.title")
    @Mapping(target = "posterUrl", source = "cartItem.movie.posterUrl")
    @Mapping(target = "genre", source = "cartItem.movie.genre")
    @Mapping(target = "price", source = "cartItem.movie.price")
    @Mapping(target = "rating", source = "cartItem.movie.rating")
    @Mapping(target = "releaseDate", source = "cartItem.movie.releaseDate")
    @Mapping(target = "currencyCode", ignore = true)
    @Mapping(target = "inFavourites", source = "userStatus.inFavourites")
    @Mapping(target = "bought", source = "userStatus.bought")
    @Mapping(target = "quantity", source = "cartItem.quantity")
    @Mapping(target = "formattedDuration",
            source = "cartItem.movie.duration",
            qualifiedByName = "durationToString")
    CartItemViewDTO toOwnerView(CartItem cartItem, UserStatus userStatus);
}