package com.example.demo.mapper.entity;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class RatedMapper {

    public ReviewViewDTO toReviewViewWithOwn(RatedMovie ratedMovie, Long currentUserId) {
        if (ratedMovie == null) {
            return null;
        }
        String username = getUsername(ratedMovie);
        boolean isOwn = ratedMovie.getUser() != null &&
                ratedMovie.getUser().getId().equals(currentUserId);
        return ReviewViewDTO.builder()
                .movieId(ratedMovie.getMovie() != null ? ratedMovie.getMovie().getId() : null)
                .username(username)
                .ratingValue(ratedMovie.getRatingValue())
                .reviewText(ratedMovie.getReview())
                .addedAt(ratedMovie.getAddedAt())
                .own(isOwn)
                .build();
    }

    public String getUsername(RatedMovie ratedMovie) {
        if (ratedMovie == null) {
            return "Удалённый аккаунт";
        }
        User user = ratedMovie.getUser();
        if (user == null || user.getDeletableState().isDeleted()) {
            return "Удалённый аккаунт";
        }
        return user.getUsername();
    }

    public RatedMovieViewDTO toOwnerView(RatedMovie ratedMovie, Long currentUserId) {
        if (ratedMovie == null) {
            return null;
        }
        return RatedMovieViewDTO.builder()
                .id(ratedMovie.getMovie().getId())
                .title(ratedMovie.getMovie().getTitle())
                .posterUrl(ratedMovie.getMovie().getPosterUrl())
                .genre(ratedMovie.getMovie().getGenre())
                .rating(ratedMovie.getMovie().getRating())
                .releaseDate(ratedMovie.getMovie().getReleaseDate())
                .ratingValue(ratedMovie.getRatingValue())
                .review(ratedMovie.getReview())
                .addedAt(ratedMovie.getAddedAt())
                .build();
    }

    @Mapping(target = "id", source = "movie.id")
    @Mapping(target = "rating", source = "ratingValue")
    @Mapping(target = "review", source = "review")
    public abstract RatedMovieFormDTO toFormView(RatedMovie ratedMovie);
}