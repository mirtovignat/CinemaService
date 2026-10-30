package com.example.demo.mapper.entity;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.util.DateTimeMapperUtil;
import com.example.demo.mapper.util.RatingMapper;
import com.example.demo.model.entity.RatedMovie;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class RatedMapper {

    public ReviewViewDTO toReviewViewWithOwn(RatedMovie ratedMovie, Long currentUserId) {
        if (ratedMovie == null) return null;
        boolean isDeleted = isAuthorDeleted(ratedMovie);
        return ReviewViewDTO.builder()
                .movieId(ratedMovie.getMovie() != null ? ratedMovie.getMovie().getId() : null)
                .username(ratedMovie.getUser() != null ? ratedMovie.getUser().getUsername() : null)
                .ratingValue(RatingMapper.normalizeRating(ratedMovie.getRatingValue()))
                .reviewText(ratedMovie.getReview())
                .addedAt(DateTimeMapperUtil.toUtcIso(ratedMovie.getAddedAt()))
                .own(ratedMovie.getUser() != null && ratedMovie.getUser().getId().equals(currentUserId))
                .deleted(isDeleted)
                .build();
    }

    public boolean isAuthorDeleted(RatedMovie ratedMovie) {
        if (ratedMovie == null || ratedMovie.getUser() == null) return true;
        String username = ratedMovie.getUser().getUsername();
        return ratedMovie.getUser().getDeletableState().isDeleted()
                || (username != null && username.startsWith("deleted_"));
    }

    public RatedMovieViewDTO toOwnerView(RatedMovie ratedMovie) {
        if (ratedMovie == null) return null;
        var movie = ratedMovie.getMovie();
        String genre = (movie.getGenre() != null && !movie.getGenre().isEmpty())
                ? String.join(", ", movie.getGenre()) : null;
        return RatedMovieViewDTO.builder()
                .id(movie.getId())
                .title(movie.getTitle())
                .posterUrl(movie.getPosterUrl())
                .genre(genre)
                .rating(RatingMapper.normalizeRating(movie.getRating()))
                .releaseDate(movie.getReleaseDate())
                .ratingValue(RatingMapper.normalizeRating(ratedMovie.getRatingValue()))
                .review(ratedMovie.getReview())
                .addedAt(DateTimeMapperUtil.toUtcIso(ratedMovie.getAddedAt()))
                .build();
    }

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "rating",
            expression = "java(com.example.demo.mapper.util.RatingMapper.normalizeRating(ratedMovie.getRatingValue()))")
    @Mapping(target = "review", source = "review")
    public abstract RatedMovieFormDTO toFormView(RatedMovie ratedMovie);
}