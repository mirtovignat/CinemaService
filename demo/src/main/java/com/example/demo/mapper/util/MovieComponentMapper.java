package com.example.demo.mapper.util;

import com.example.demo.dto.base.*;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.PurchasedMovie;
import org.mapstruct.Mapper;
import org.mapstruct.Named;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring")
public class MovieComponentMapper {

    @Autowired
    private DurationMapper durationMapper;

    @Autowired
    private GenreMapper genreMapper;

    @Named("toIdentity")
    public MovieIdentity toIdentity(Movie movie) {
        return movie == null ? null : new MovieIdentity(movie.getId());
    }

    @Named("toTitled")
    public MovieTitled toTitled(Movie movie) {
        return movie == null ? null : new MovieTitled(movie.getTitle());
    }

    @Named("toPricing")
    public MoviePricing toPricing(Movie movie) {
        return movie == null ? null : new MoviePricing(movie.getPrice());
    }

    @Named("toMedia")
    public MovieMedia toMedia(Movie movie) {
        if (movie == null) return null;
        return MovieMedia.builder()
                .posterUrl(movie.getPosterUrl())
                .genre(genreMapper.toString(movie.getGenre()))
                .build();
    }

    @Named("toRating")
    public MovieRating toRating(Movie movie) {
        return movie == null ? null : new MovieRating(movie.getRating());
    }

    @Named("toRelease")
    public MovieRelease toRelease(Movie movie) {
        return movie == null ? null : new MovieRelease(movie.getReleaseDate());
    }

    @Named("toMeta")
    public MovieMeta toMeta(Movie movie) {
        if (movie == null) return null;
        return MovieMeta.builder()
                .director(movie.getDirector())
                .formattedDuration(durationMapper.toString(movie.getDuration()))
                .description(movie.getDescription())
                .ratingsCount(movie.getRatingsCount() != null ? movie.getRatingsCount().longValue() : null)
                .build();
    }

    @Named("toMetaWithoutDescription")
    public MovieMeta toMetaWithoutDescription(Movie movie) {
        if (movie == null) return null;
        return MovieMeta.builder()
                .director(movie.getDirector())
                .formattedDuration(durationMapper.toString(movie.getDuration()))
                .description(null)
                .ratingsCount(movie.getRatingsCount() != null ? movie.getRatingsCount().longValue() : null)
                .build();
    }

    @Named("toMetaForLibrarian")
    public MovieMeta toMetaForLibrarian(Movie movie) {
        if (movie == null) return null;
        return MovieMeta.builder()
                .director(movie.getDirector())
                .formattedDuration(durationMapper.toString(movie.getDuration()))
                .description(null)
                .ratingsCount(null)
                .build();
    }

    @Named("toPricingSnapshot")
    public MoviePricing toPricingSnapshot(PurchasedMovie purchasedMovie) {
        return purchasedMovie == null ? null : new MoviePricing(purchasedMovie.getPriceSnapshot());
    }
}