package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.util.DateTimeMapper;
import com.example.demo.mapper.util.RatingMapper;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.model.entity.User;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class ReviewViewAssembler {

    private final RatingMapper ratingMapper;
    private final DateTimeMapper dateTimeMapper;

    public ReviewViewDTO assemble(RatedMovie ratedMovie, Long viewerId) {
        if (ratedMovie == null) {
            return null;
        }
        User author = ratedMovie.getUser();

        return ReviewViewDTO.builder()
                .movieId(ratedMovie.getMovie() != null ? ratedMovie.getMovie().getId() : null)
                .username(author != null ? author.getUsername() : null)
                .ratingValue(ratingMapper.normalizeRating(ratedMovie.getRatingValue()))
                .reviewText(ratedMovie.getReview())
                .addedAt(dateTimeMapper.toUtcIso(ratedMovie.getAddedAt()))
                .own(author != null && author.getId().equals(viewerId))
                .deleted(author != null && author.getDeletableState().isDeleted())
                .build();
    }
}