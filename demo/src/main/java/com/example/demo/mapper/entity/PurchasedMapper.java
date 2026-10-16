package com.example.demo.mapper.entity;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.model.entity.PurchasedMovie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PurchasedMapper {

    @Mapping(target = "id", source = "purchasedMovie.movie.id")
    @Mapping(target = "title", source = "purchasedMovie.movie.title")
    @Mapping(target = "posterUrl", source = "purchasedMovie.movie.posterUrl")
    @Mapping(target = "genre", source = "purchasedMovie.movie.genre")
    @Mapping(target = "rating", source = "purchasedMovie.movie.rating")
    @Mapping(target = "releaseDate", source = "purchasedMovie.movie.releaseDate")
    @Mapping(target = "ageRating", source = "purchasedMovie.movie.ageRating")
    @Mapping(target = "watched", constant = "true")
    @Mapping(target = "addedAt", source = "watchedAt")
    @Mapping(target = "historyItemId", source = "id")
    @Mapping(target = "director", source = "purchasedMovie.movie.director")
    LibrarianMovieViewDTO toLibrarianViewFromHistoryItem(WatchHistoryItem item);

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "priceSnapshot", source = "priceSnapshot")
    @Mapping(target = "addedAt", source = "addedAt")
    HistoricalMovieViewDTO toOwnerViewFromHistorical(PurchasedMovie purchasedMovie);

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "posterUrl", source = "movie.posterUrl")
    @Mapping(target = "genre", source = "movie.genre")
    @Mapping(target = "rating", source = "movie.rating")
    @Mapping(target = "releaseDate", source = "movie.releaseDate")
    @Mapping(target = "ageRating", source = "movie.ageRating")
    @Mapping(target = "watched",
            expression = "java(!purchasedMovie.getWatchedHistory().isEmpty())")
    @Mapping(target = "addedAt", source = "addedAt")
    @Mapping(target = "historyItemId", ignore = true)
    @Mapping(target = "director", source = "movie.director")
    LibrarianMovieViewDTO toOwnerViewFromLibrarian(PurchasedMovie purchasedMovie);
}