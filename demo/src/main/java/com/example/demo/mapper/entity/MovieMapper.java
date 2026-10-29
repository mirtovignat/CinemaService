package com.example.demo.mapper.entity;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieDetailsViewDTO;
import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.dto.movie.tmdb.TMDBMovieDTO;
import com.example.demo.dto.movie.tmdb.TMDBSearchResultDTO;
import com.example.demo.mapper.util.DurationMapper;
import com.example.demo.mapper.util.TmdbMappingUtil;
import com.example.demo.model.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

@Mapper(componentModel = "spring", uses = {DurationMapper.class, TmdbMappingUtil.class})
public interface MovieMapper {

    @Mapping(target = "formattedDuration",
            source = "duration",
            qualifiedByName = "durationToString")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "currencyCode", ignore = true)
    MovieCardViewDTO toCard(Movie movie);

    @Mapping(target = "formattedDuration",
            source = "duration",
            qualifiedByName = "durationToString")
    @Mapping(target = "currencyCode", ignore = true)
    @Mapping(target = "bought", ignore = true)
    @Mapping(target = "inCart", ignore = true)
    @Mapping(target = "inFavourites", ignore = true)
    @Mapping(target = "ownRating", ignore = true)
    MovieDetailsViewDTO toDetails(Movie movie);

    MovieSearchDTO toMovieSearch(Movie movie);

    @Mapping(target = "tmdbId", source = "id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "originalTitle", source = "originalTitle")
    @Mapping(target = "releaseDate", source = "releaseDate")
    @Mapping(target = "posterUrl", source = "posterPath", qualifiedByName = "posterPathToUrl")
    TMDBSearchResultDTO toTmdbSearchResult(TMDBMovieDTO tmdbMovie);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "ratingSort", ignore = true)
    @Mapping(target = "posterUrl", ignore = true)
    @Mapping(target = "rating", ignore = true)
    @Mapping(target = "ratingsCount", constant = "0")
    @Mapping(target = "cartItems", ignore = true)
    @Mapping(target = "purchases", ignore = true)
    @Mapping(target = "favourites", ignore = true)
    @Mapping(target = "ratings", ignore = true)
    @Mapping(target = "tmdbId", source = "tmdbMovie.id")
    @Mapping(target = "title", source = "tmdbMovie.title")
    @Mapping(target = "description", source = "tmdbMovie.overview")
    @Mapping(target = "ageRating", constant = "16")
    @Mapping(target = "price", source = "price")
    @Mapping(target = "duration", source = "tmdbMovie.runtime", qualifiedByName = "runtimeToDuration")
    @Mapping(target = "genre", source = "tmdbMovie.genres", qualifiedByName = "firstGenreName")
    @Mapping(target = "releaseDate", source = "tmdbMovie.releaseDate", qualifiedByName = "parseReleaseDate")
    @Mapping(target = "actors", source = "tmdbMovie.credits", qualifiedByName = "castToActors")
    @Mapping(target = "director", ignore = true)
    Movie fromTmdb(TMDBMovieDTO tmdbMovie, BigDecimal price);
}