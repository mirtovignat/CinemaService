package com.example.demo.service.catalog;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.RatedMovieViewDTO;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.movie.MovieService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RatedQueryService {

    private final RatedMovieRepository ratedMovieRepository;
    private final RatedMapper ratedMapper;
    private final MovieService movieService;
    private final int ratingsToLimit;

    public RatedQueryService(RatedMovieRepository ratedMovieRepository,
                             RatedMapper ratedMapper,
                             @Lazy MovieService movieService,
                             @Value("${app.rating.top-limit:4}") int ratingsToLimit) {
        this.ratedMovieRepository = ratedMovieRepository;
        this.ratedMapper = ratedMapper;
        this.movieService = movieService;
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

    public BigDecimal getMovieRating(Long movieId) {
        return movieService.getMovie(movieId).getRating();
    }

    public String getNormalizedRating(BigDecimal rating) {
        return rating != null ? rating.toString() : "-";
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

    public Map<Long, ReviewViewDTO> getOwnRatings(Long userId) {
        return ratedMovieRepository.findAllActiveByUserId(userId).stream()
                .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(ratedMovie, userId))
                .collect(Collectors.toMap(
                        ReviewViewDTO::movieId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    @Transactional(readOnly = true)
    public Map<Long, ReviewViewDTO> getOwnRatingsForMovies(Collection<Long> movieIds, Long userId) {
        if (movieIds == null || movieIds.isEmpty() || userId == null) {
            return Map.of();
        }
        Set<Long> requestedIds = new LinkedHashSet<>(movieIds);
        return getOwnRatings(userId).entrySet().stream()
                .filter(entry -> requestedIds.contains(entry.getKey()))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public ReviewViewDTO getRating(Long movieId, Long userId) {
        if (userId == null || movieId == null) {
            return null;
        }
        return ratedMovieRepository.findOwnReview(movieId, userId)
                .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(ratedMovie, userId))
                .orElse(null);
    }
}