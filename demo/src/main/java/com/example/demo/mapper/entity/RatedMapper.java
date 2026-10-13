package com.example.demo.mapper.entity;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.util.MapperUtils;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", uses = MapperUtils.class)
public abstract class RatedMapper {

    public ReviewViewDTO toReviewViewWithOwn(RatedMovie ratedMovie, Long currentUserId) {
        if (ratedMovie == null) {
            return null;
        }
        String reviewAuthorUsername = resolveReviewAuthorUsername(ratedMovie);
        boolean isOwnReview = ratedMovie.getUser() != null
                && ratedMovie.getUser().getId().equals(currentUserId);
        return ReviewViewDTO.builder()
                .movieId(ratedMovie.getMovie() != null ? ratedMovie.getMovie().getId() : null)
                .username(reviewAuthorUsername)
                .ratingValue(MapperUtils.normalizeRating(ratedMovie.getRatingValue()))
                .reviewText(ratedMovie.getReview())
                .addedAt(ratedMovie.getAddedAt())
                .own(isOwnReview)
                .build();
    }

    public String resolveReviewAuthorUsername(RatedMovie ratedMovie) {
        if (ratedMovie == null) {
            return "Удалённый аккаунт";
        }
        User reviewAuthor = ratedMovie.getUser();
        if (reviewAuthor == null || reviewAuthor.getDeletableState().isDeleted()) {
            return "Удалённый аккаунт";
        }
        return reviewAuthor.getUsername();
    }

    public RatedMovieViewDTO toOwnerView(RatedMovie ratedMovie) {
        if (ratedMovie == null) {
            return null;
        }
        return RatedMovieViewDTO.builder()
                .id(ratedMovie.getMovie().getId())
                .title(ratedMovie.getMovie().getTitle())
                .posterUrl(ratedMovie.getMovie().getPosterUrl())
                .genre(ratedMovie.getMovie().getGenre())
                .rating(MapperUtils.normalizeRating(ratedMovie.getMovie().getRating()))
                .releaseDate(ratedMovie.getMovie().getReleaseDate())
                .ratingValue(MapperUtils.normalizeRating(ratedMovie.getRatingValue()))
                .review(ratedMovie.getReview())
                .addedAt(ratedMovie.getAddedAt())
                .build();
    }

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "rating", source = "ratingValue", qualifiedByName = "normalizeRating")
    @Mapping(target = "review", source = "review")
    public abstract RatedMovieFormDTO toFormView(RatedMovie ratedMovie);
}