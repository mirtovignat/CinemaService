package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.repository.entity.RatedMovieRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RatedQueryService {

    private final RatedMovieRepository ratedMovieRepository;
    private final RatedMapper ratedMapper;
    private final ReviewViewAssembler reviewViewAssembler;
    private final int ratingsToLimit;

    public RatedQueryService(RatedMovieRepository ratedMovieRepository,
                             RatedMapper ratedMapper,
                             ReviewViewAssembler reviewViewAssembler,
                             @Value("${app.rating.top-limit:4}") int ratingsToLimit) {
        this.ratedMovieRepository = ratedMovieRepository;
        this.ratedMapper = ratedMapper;
        this.reviewViewAssembler = reviewViewAssembler;
        this.ratingsToLimit = ratingsToLimit;
    }

    public RatedMovieFormDTO getPreFilledForm(Long movieId, Long userId) {
        if (movieId == null || userId == null) {
            return RatedMovieFormDTO.builder()
                    .id(movieId)
                    .build();
        }
        return ratedMovieRepository.findByMovieIdAndUserId(movieId, userId)
                .map(ratedMapper::toFormView)
                .orElse(RatedMovieFormDTO.builder()
                        .id(movieId)
                        .build());
    }

    public Long getReviewsCountForMovie(Long movieId) {
        return ratedMovieRepository.countByMovieId(movieId);
    }

    public List<RatedMovieViewDTO> getTopNRatedMovies(Long userId) {
        return ratedMovieRepository.findTopNByUserIdOrderByRatingValueDesc(
                        userId,
                        PageRequest.of(0, ratingsToLimit)
                ).stream()
                .map(ratedMapper::toOwnerView)
                .toList();
    }

    public ReviewViewDTO getRating(Long movieId, Long userId) {
        if (userId == null || movieId == null) {
            return null;
        }
        return ratedMovieRepository.findByMovieIdAndUserId(movieId, userId)
                .map(ratedMovie -> reviewViewAssembler.assemble(ratedMovie, userId))
                .orElse(null);
    }
}