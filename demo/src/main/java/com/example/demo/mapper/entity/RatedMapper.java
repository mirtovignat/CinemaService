package com.example.demo.mapper.entity;

import com.example.demo.dto.base.UserOwnership;
import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.util.MovieComponentMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.springframework.beans.factory.annotation.Autowired;

@Mapper(componentModel = "spring", uses = {MovieComponentMapper.class})
public abstract class RatedMapper {

    @Autowired
    protected MovieComponentMapper componentMapper;

    public ReviewViewDTO toReviewViewWithOwn(RatedMovie ratedMovie, Long currentUserId) {
        if (ratedMovie == null) {
            return null;
        }
        String username = getUsername(ratedMovie);
        boolean isOwn = ratedMovie.getUser() != null &&
                ratedMovie.getUser().getId().equals(currentUserId);
        return ReviewViewDTO.builder()
                .username(username)
                .ratingValue(ratedMovie.getRatingValue())
                .reviewText(ratedMovie.getReview())
                .ownership(UserOwnership.builder()
                        .own(isOwn)
                        .addedAt(ratedMovie.getAddedAt())
                        .build())
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

    public UserOwnership toUserOwnership(RatedMovie ratedMovie, Long currentUserId) {
        if (ratedMovie == null) {
            return UserOwnership.builder()
                    .own(false)
                    .addedAt(null)
                    .build();
        }
        boolean isOwn = ratedMovie.getUser() != null &&
                ratedMovie.getUser().getId().equals(currentUserId);
        return UserOwnership.builder()
                .own(isOwn)
                .addedAt(ratedMovie.getAddedAt())
                .build();
    }

    public RatedMovieViewDTO toOwnerView(RatedMovie ratedMovie, Long currentUserId) {
        if (ratedMovie == null) {
            return null;
        }
        return RatedMovieViewDTO.builder()
                .id(componentMapper.toIdentity(ratedMovie.getMovie()))
                .title(componentMapper.toTitled(ratedMovie.getMovie()))
                .media(componentMapper.toMedia(ratedMovie.getMovie()))
                .rating(componentMapper.toRating(ratedMovie.getMovie()))
                .release(componentMapper.toRelease(ratedMovie.getMovie()))
                .ownership(toUserOwnership(ratedMovie, currentUserId))
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