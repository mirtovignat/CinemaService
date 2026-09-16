package com.example.demo.mapper.entity;

import com.example.demo.dto.movie.MovieCardDetailsViewDTO;
import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.mapper.util.MovieComponentMapper;
import com.example.demo.model.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {MovieComponentMapper.class})
public interface MovieMapper {

    @Mapping(target = "id", source = "movie", qualifiedByName = "toIdentity")
    @Mapping(target = "title", source = "movie", qualifiedByName = "toTitled")
    @Mapping(target = "price", source = "movie", qualifiedByName = "toPricing")
    @Mapping(target = "media", source = "movie", qualifiedByName = "toMedia")
    @Mapping(target = "rating", source = "movie", qualifiedByName = "toRating")
    @Mapping(target = "release", source = "movie", qualifiedByName = "toRelease")
    @Mapping(target = "meta", source = "movie", qualifiedByName = "toMetaWithoutDescription")
    MovieCardViewDTO toCard(Movie movie);

    @Mapping(target = "id", source = "movie", qualifiedByName = "toIdentity")
    @Mapping(target = "title", source = "movie", qualifiedByName = "toTitled")
    @Mapping(target = "price", source = "movie", qualifiedByName = "toPricing")
    @Mapping(target = "media", source = "movie", qualifiedByName = "toMedia")
    @Mapping(target = "rating", source = "movie", qualifiedByName = "toRating")
    @Mapping(target = "release", source = "movie", qualifiedByName = "toRelease")
    @Mapping(target = "meta", source = "movie", qualifiedByName = "toMeta")
    MovieCardDetailsViewDTO toDetails(Movie movie);

    @Mapping(target = "id", source = "movie", qualifiedByName = "toIdentity")
    @Mapping(target = "title", source = "movie", qualifiedByName = "toTitled")
    @Mapping(target = "release", source = "movie", qualifiedByName = "toRelease")
    @Mapping(target = "media", source = "movie", qualifiedByName = "toMedia")
    @Mapping(target = "director", source = "movie.director")
    MovieSearchDTO toMovieSearch(Movie movie);
}