package com.example.demo.service.rated;

import com.example.demo.dto.catalog.ReviewCursor;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.dto.catalog.ReviewsPageRequest;
import com.example.demo.dto.catalog.ReviewsQueryDTO;
import com.example.demo.dto.response.ReviewsSliceResponse;
import com.example.demo.mapper.entity.RatedMapper;
import com.example.demo.model.entity.Movie;
import com.example.demo.model.entity.RatedMovie;
import com.example.demo.repository.entity.RatedMovieRepository;
import com.example.demo.service.movie.MovieService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ReviewsQueryService {

    private final RatedMovieRepository ratedRepository;
    private final RatedMapper ratedMapper;
    private final MovieService movieService;

    @Transactional(readOnly = true)
    public ReviewsSliceResponse buildReviewsPage(Long movieId,
                                                 Long viewerUserId,
                                                 ReviewsPageRequest request) {
        Movie movie = movieService.getMovie(movieId);
        Long safeViewerId = request.safeViewerId(viewerUserId);
        int size = request.safeSize();
        ReviewCursor cursor = request.getCursor();

        PageRequest limit = PageRequest.of(0, size);
        Slice<RatedMovie> slice = fetchSlice(movieId, safeViewerId, request, cursor, limit);

        List<ReviewViewDTO> reviews = slice.getContent().stream()
                .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(ratedMovie, viewerUserId))
                .toList();

        ReviewViewDTO ownReview = cursor == null
                ? resolveOwnReview(movieId, viewerUserId)
                : null;

        String nextCursor = resolveNextCursor(slice);

        return ReviewsSliceResponse.builder()
                .reviews(reviews)
                .ownReview(ownReview)
                .hasMore(slice.hasNext())
                .nextCursor(nextCursor)
                .totalReviews(movie.getRatingsCount() != null
                        ? movie.getRatingsCount().longValue()
                        : 0L)
                .avgRating(movie.getRating())
                .movieTitle(movie.getTitle())
                .movieId(movieId)
                .sort(request.getSort().name())
                .pageSize(size)
                .build();
    }

    private Slice<RatedMovie> fetchSlice(Long movieId,
                                         Long viewerId,
                                         ReviewsPageRequest request,
                                         ReviewCursor cursor,
                                         PageRequest limit) {
        boolean ascending = request.getSort() == ReviewsQueryDTO.SortDirection.ASC;

        if (cursor == null) {
            return ascending
                    ? ratedRepository.findReviewsAscFirst(movieId, viewerId, limit)
                    : ratedRepository.findReviewsDescFirst(movieId, viewerId, limit);
        }

        return ascending
                ? ratedRepository.findReviewsAscAfter(
                movieId, viewerId, cursor.addedAt(), cursor.userId(), limit)
                : ratedRepository.findReviewsDescAfter(
                movieId, viewerId, cursor.addedAt(), cursor.userId(), limit);
    }

    private String resolveNextCursor(Slice<RatedMovie> slice) {
        if (!slice.hasNext() || slice.getContent().isEmpty()) {
            return null;
        }
        RatedMovie last = slice.getContent().get(slice.getContent().size() - 1);
        return ReviewCursor.builder()
                .addedAt(last.getAddedAt())
                .userId(last.getUser().getId())
                .build()
                .encode();
    }

    private ReviewViewDTO resolveOwnReview(Long movieId, Long viewerUserId) {
        if (viewerUserId == null) {
            return null;
        }
        return ratedRepository.findOwnReview(movieId, viewerUserId)
                .map(ratedMovie -> ratedMapper.toReviewViewWithOwn(ratedMovie, viewerUserId))
                .orElse(null);
    }
}