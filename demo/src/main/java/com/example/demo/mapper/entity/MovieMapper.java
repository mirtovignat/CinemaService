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

    @Mapping(target = "formattedDuration",
            source = "movie.duration",
            qualifiedByName = "durationToString")
    @Mapping(target = "description", ignore = true)
    @Mapping(target = "currencyCode", ignore = true)
    @Mapping(target = "ratingsCount", source = "ratingsCount")
    MovieCardViewDTO toCard(Movie movie, long ratingsCount);

    @Mapping(target = "formattedDuration",
            source = "movie.duration",
            qualifiedByName = "durationToString")
    @Mapping(target = "currencyCode", ignore = true)
    @Mapping(target = "ratingsCount", source = "ratingsCount")
    MovieDetailsViewDTO toDetails(Movie movie, long ratingsCount);

    MovieSearchDTO toMovieSearch(Movie movie);
}