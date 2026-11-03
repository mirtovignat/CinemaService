package com.example.demo.mapper.entity;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.mapper.util.DateTimeMapper;
import com.example.demo.mapper.util.RatingMapper;
import com.example.demo.model.entity.RatedMovie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {RatingMapper.class, DateTimeMapper.class})
public abstract class RatedMapper {

    @Autowired
    protected RatingMapper ratingMapper;

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "title", source = "movie.title")
    @Mapping(target = "posterUrl", source = "movie.posterUrl")
    @Mapping(target = "genre", source = "movie.genre")
    @Mapping(target = "rating", source = "movie.rating", qualifiedByName = "normalizeRating")
    @Mapping(target = "releaseDate", source = "movie.releaseDate")
    @Mapping(target = "ratingValue", source = "ratingValue", qualifiedByName = "normalizeRating")
    @Mapping(target = "review", source = "review")
    @Mapping(target = "addedAt", source = "addedAt", qualifiedByName = "localDateTimeToIso")
    public abstract RatedMovieViewDTO toOwnerView(RatedMovie ratedMovie);

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "rating", source = "ratingValue", qualifiedByName = "normalizeRating")
    public abstract RatedMovieFormDTO toFormView(RatedMovie ratedMovie);
}