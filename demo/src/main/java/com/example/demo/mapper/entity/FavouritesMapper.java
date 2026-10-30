package com.example.demo.mapper.entity;

import com.example.demo.dto.base.UserStatus;
import com.example.demo.dto.catalog.FavouriteMovieViewDTO;
import com.example.demo.mapper.util.DurationMapper;
import com.example.demo.model.entity.FavouriteMovie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DurationMapper.class})
public interface FavouritesMapper {

    @Mapping(target = "id", source = "favouriteMovie.movie.id")
    @Mapping(target = "title", source = "favouriteMovie.movie.title")
    @Mapping(target = "posterUrl", source = "favouriteMovie.movie.posterUrl")
    @Mapping(target = "genre", expression = "java(favouriteMovie.getMovie().getGenre() != null && !favouriteMovie.getMovie().getGenre().isEmpty() ? String.join(\", \", favouriteMovie.getMovie().getGenre()) : null)")
    @Mapping(target = "price", source = "favouriteMovie.movie.price")
    @Mapping(target = "rating", source = "favouriteMovie.movie.rating")
    @Mapping(target = "releaseDate", source = "favouriteMovie.movie.releaseDate")
    @Mapping(target = "currencyCode", ignore = true)
    @Mapping(target = "inCart", source = "userStatus.inCart")
    @Mapping(target = "bought", source = "userStatus.bought")
    @Mapping(target = "formattedDuration",
            source = "favouriteMovie.movie.duration",
            qualifiedByName = "durationToString")
    FavouriteMovieViewDTO toOwnerView(FavouriteMovie favouriteMovie, UserStatus userStatus);
}