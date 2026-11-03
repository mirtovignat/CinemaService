package com.example.demo.mapper.entity;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.util.DateTimeMapper;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.model.entity.PurchasedMovie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {DateTimeMapper.class})
public abstract class PurchasedMapper {

    @Autowired
    protected DateTimeMapper dateTimeMapper;

    @Mapping(target = "id", source = "purchasedMovie.movie.id")
    @Mapping(target = "title", source = "purchasedMovie.movie.title")
    @Mapping(target = "posterUrl", source = "purchasedMovie.movie.posterUrl")
    @Mapping(target = "genre", source = "purchasedMovie.movie.genre")
    @Mapping(target = "rating", source = "purchasedMovie.movie.rating")
    @Mapping(target = "releaseDate", source = "purchasedMovie.movie.releaseDate")
    @Mapping(target = "ageRating", source = "purchasedMovie.movie.ageRating")
    @Mapping(target = "watched", constant = "true")
    @Mapping(target = "addedAt", source = "watchedAt", qualifiedByName = "localDateTimeToIso")
    @Mapping(target = "historyItemId", source = "id")
    @Mapping(target = "director", source = "purchasedMovie.movie.director")
    public abstract LibrarianMovieViewDTO toLibrarianViewFromHistoryItem(WatchHistoryItem watchHistoryItem);

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "priceSnapshot", source = "priceSnapshot")
    @Mapping(target = "addedAt", source = "addedAt", qualifiedByName = "localDateTimeToIso")
    public abstract HistoricalMovieViewDTO toOwnerViewFromHistorical(PurchasedMovie purchasedMovie);

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "posterUrl", source = "movie.posterUrl")
    @Mapping(target = "genre", source = "movie.genre")
    @Mapping(target = "rating", source = "movie.rating")
    @Mapping(target = "releaseDate", source = "movie.releaseDate")
    @Mapping(target = "ageRating", source = "movie.ageRating")
    @Mapping(target = "watched",
            expression = "java(!purchasedMovie.getWatchedHistory().isEmpty())")
    @Mapping(target = "addedAt", source = "addedAt", qualifiedByName = "localDateTimeToIso")
    @Mapping(target = "historyItemId", ignore = true)
    @Mapping(target = "director", source = "movie.director")
    public abstract LibrarianMovieViewDTO toOwnerViewFromLibrarian(PurchasedMovie purchasedMovie);
}