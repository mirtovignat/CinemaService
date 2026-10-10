package com.example.demo.mapper.entity;

import com.example.demo.dto.movie.MovieCardViewDTO;
import com.example.demo.dto.movie.MovieDetailsViewDTO;
import com.example.demo.dto.movie.MovieSearchDTO;
import com.example.demo.mapper.util.DurationMapper;
import com.example.demo.model.entity.Movie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = {DurationMapper.class})
public interface MovieMapper {

    @Mapping(target = "formattedDuration", source = "duration", qualifiedByName = "durationToString")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "currencyCode", ignore = true)
    MovieCardViewDTO toCard(Movie movie);

    @Mapping(target = "formattedDuration", source = "duration", qualifiedByName = "durationToString")
    @Mapping(target = "currencyCode", ignore = true)
    @Mapping(target = "bought", ignore = true)
    @Mapping(target = "inCart", ignore = true)
    @Mapping(target = "inFavourites", ignore = true)
    @Mapping(target = "ownRating", ignore = true)
    MovieDetailsViewDTO toDetails(Movie movie);

    MovieSearchDTO toMovieSearch(Movie movie);
}