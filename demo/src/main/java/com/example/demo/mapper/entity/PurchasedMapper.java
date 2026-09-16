package com.example.demo.mapper.entity;

import com.example.demo.dto.catalog.HistoricalMovieViewDTO;
import com.example.demo.dto.catalog.LibrarianMovieViewDTO;
import com.example.demo.mapper.util.MovieComponentMapper;
import com.example.demo.model.base.WatchHistoryItem;
import com.example.demo.model.entity.PurchasedMovie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MovieComponentMapper.class})
public interface PurchasedMapper {

    @Mapping(target = "id", source = "purchasedMovie.movie", qualifiedByName = "toIdentity")
    @Mapping(target = "title", source = "purchasedMovie.movie", qualifiedByName = "toTitled")
    @Mapping(target = "media", source = "purchasedMovie.movie", qualifiedByName = "toMedia")
    @Mapping(target = "rating", source = "purchasedMovie.movie", qualifiedByName = "toRating")
    @Mapping(target = "release", source = "purchasedMovie.movie", qualifiedByName = "toRelease")
    @Mapping(target = "meta", source = "purchasedMovie.movie", qualifiedByName = "toMetaForLibrarian")
    @Mapping(target = "watched", constant = "true")
    @Mapping(target = "addedAt", source = "watchedAt")
    @Mapping(target = "historyItemId", source = "id")
    LibrarianMovieViewDTO toLibrarianViewFromHistoryItem(WatchHistoryItem item);

    @Mapping(target = "id", source = "movie", qualifiedByName = "toIdentity")
    @Mapping(target = "title", source = "movie", qualifiedByName = "toTitled")
    @Mapping(target = "priceSnapshot", source = "priceSnapshot")
    @Mapping(target = "addedAt", source = "addedAt")
    HistoricalMovieViewDTO toOwnerViewFromHistorical(PurchasedMovie purchasedMovie);

    @Mapping(target = "id", source = "movie", qualifiedByName = "toIdentity")
    @Mapping(target = "title", source = "movie", qualifiedByName = "toTitled")
    @Mapping(target = "media", source = "movie", qualifiedByName = "toMedia")
    @Mapping(target = "rating", source = "movie", qualifiedByName = "toRating")
    @Mapping(target = "release", source = "movie", qualifiedByName = "toRelease")
    @Mapping(target = "meta", source = "movie", qualifiedByName = "toMetaForLibrarian")
    @Mapping(target = "watched", expression = "java(!purchasedMovie.getWatchedHistory().isEmpty())")
    @Mapping(target = "addedAt", source = "addedAt")
    @Mapping(target = "historyItemId", ignore = true)
    LibrarianMovieViewDTO toOwnerViewFromLibrarian(PurchasedMovie purchasedMovie);
}