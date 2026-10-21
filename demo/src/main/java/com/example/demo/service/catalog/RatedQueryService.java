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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class RatedQueryService {

    private final RatedMovieRepository ratedRepository;
    private final RatedMapper ratedMapper;
    private final MovieService movieService;
    private final int ratingsToLimit;

    public RatedQueryService(
            RatedMovieRepository ratedRepository,
            RatedMapper ratedMapper,
            @Lazy MovieService movieService,
            @Value("${app.rating.top-limit:4}") int ratingsToLimit
    ) {
        this.ratedRepository = ratedRepository;
        this.ratedMapper = ratedMapper;
        this.movieService = movieService;
        this.ratingsToLimit = ratingsToLimit;
    }

    public long countByUserId(Long userId) {
        Long count = ratedRepository.countByUserId(userId);
        return count == null ? 0L : count;
    }

    public RatedMovieFormDTO getPreFilledForm(Long movieId, Long userId) {
        return ratedRepository.findByMovieIdAndUserId(movieId, userId)
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
        return ratedRepository.countByMovieId(movieId);
    }

    public List<RatedMovieViewDTO> getTopNRatedMovies(Long userId) {
        return ratedRepository.findTopNByUserIdOrderByRatingValueDesc(
                        userId,
                        PageRequest.of(0, ratingsToLimit)
                ).stream()
                .map(ratedMapper::toOwnerView)
                .toList();
    }

    @Transactional(readOnly = true)
    public Map<Long, ReviewViewDTO> getOwnRatings(Long userId) {
        return ratedRepository.findAllActiveByUserId(userId).stream()
                .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(ratedMovie, userId))
                .collect(Collectors.toMap(
                        ReviewViewDTO::movieId,
                        Function.identity(),
                        (existing, replacement) -> existing,
                        LinkedHashMap::new
                ));
    }

    public ReviewViewDTO getRating(Long movieId, Long userId) {
        if (userId == null || movieId == null) {
            return null;
        }
        return ratedRepository.findOwnReview(movieId, userId)
                .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(ratedMovie, userId))
                .orElse(null);
    }
}