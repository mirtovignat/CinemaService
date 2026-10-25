package com.example.demo.controller;

import com.example.demo.cursor.SortDirection;
import com.example.demo.cursor.context.ReviewCursorContext;
import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.dto.catalog.ReviewsQueryDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.response.ReviewsPageResponse;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.catalog.RatedCommandService;
import com.example.demo.service.catalog.RatedQueryService;
import com.example.demo.service.cursor.ReviewCursorService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.resolver.ReviewSortResolver;
import com.example.demo.web.util.ApiResponseFactory;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@AllArgsConstructor
@Controller
@RequestMapping("/rated")
public class RatedController {

    private final RatedCommandService ratedCommandService;
    private final RatedQueryService ratedQueryService;
    private final ReviewCursorService reviewCursorService;
    private final UserQueryService userQueryService;
    private final MovieService movieService;
    private final MessageUtils messageUtils;
    private final ReviewSortResolver reviewSortResolver;
    private final ApiResponseFactory apiResponseFactory;

    @GetMapping("/{id}/reviews")
    public String getReviews(@PathVariable Long id,
                             @ModelAttribute ReviewsQueryDTO reviewsQueryDTO,
                             @CurrentUserId(required = false) Long userId,
                             @CookieValue(
                                     value = ReviewSortResolver.SORT_COOKIE_NAME,
                                     required = false
                             )
                             String sortCookie,
                             HttpServletResponse httpServletResponse,
                             Model model) {

        SortDirection resolvedSortDirection = reviewSortResolver.resolveAndPersist(
                reviewsQueryDTO, sortCookie, httpServletResponse);

        CursorPageResponse<ReviewViewDTO> cursorPageResponse =
                reviewCursorService.page(
                        reviewsQueryDTO.cursor(),
                        resolvedSortDirection,
                        null,
                        ReviewCursorContext.builder()
                                .movieId(id)
                                .viewerId(userId)
                                .build());

        ReviewsPageResponse reviewsPageResponse = ReviewsPageResponse.builder()
                .cursorPageResponse(cursorPageResponse)
                .reviewViewDTO(userId != null ? ratedQueryService.getRating(id, userId) : null)
                .movieId(id)
                .movieTitle(movieService.getMovie(id).getTitle())
                .avgRating(ratedQueryService.getMovieRating(id))
                .totalReviews(ratedQueryService.getReviewsCountForMovie(id))
                .currentUsername(userId != null ? userQueryService.getUsername(userId) : null)
                .build();

        model.addAttribute("reviewsPageResponse", reviewsPageResponse);
        return "catalog/reviews";
    }

    @GetMapping("/{id}/reviews/more")
    @ResponseBody
    public CursorPageResponse<ReviewViewDTO> getReviewsMore(@PathVariable Long id,
                                                            @ModelAttribute ReviewsQueryDTO reviewsQueryDTO,
                                                            @CurrentUserId(required = false) Long userId,
                                                            @CookieValue(
                                                                    value = ReviewSortResolver.SORT_COOKIE_NAME,
                                                                    required = false
                                                            )
                                                            String sortCookie) {
        SortDirection resolvedSortDirection = reviewSortResolver.resolve(reviewsQueryDTO, sortCookie);

        return reviewCursorService.page(
                reviewsQueryDTO.cursor(),
                resolvedSortDirection,
                null,
                ReviewCursorContext.builder()
                        .movieId(id)
                        .viewerId(userId)
                        .build());
    }

    @PostMapping("/{id}/reviews/delete")
    public String deleteReview(@PathVariable("id") Long movieId,
                               @CurrentUserId Long userId,
                               RedirectAttributes redirectAttributes) {
        ratedCommandService.deleteRating(movieId, userId);
        String successMessage = messageUtils.getSuccess(SuccessCode.REVIEW_HAS_BEEN_DELETED);
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return "redirect:/rated/" + movieId + "/reviews";
    }

    @GetMapping("/form/{movieId}")
    @ResponseBody
    public ResponseEntity<RatedMovieFormDTO> getMyReviewForm(@PathVariable Long movieId,
                                                             @CurrentUserId Long userId) {
        return ResponseEntity.ok(ratedQueryService.getPreFilledForm(movieId, userId));
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> addRating(@Valid @ModelAttribute RatedMovieFormDTO ratedMovieFormDTO,
                                                       @CurrentUserId Long userId) {
        ratedCommandService.addOrUpdateRating(ratedMovieFormDTO.id(), userId, ratedMovieFormDTO);
        return apiResponseFactory.ok(SuccessCode.REVIEW_HAS_BEEN_SAVED);
    }

    @PostMapping("/edit")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> editRating(@Valid @ModelAttribute RatedMovieFormDTO ratedMovieFormDTO,
                                                        @CurrentUserId Long userId) {
        ratedCommandService.addOrUpdateRating(ratedMovieFormDTO.id(), userId, ratedMovieFormDTO);
        return apiResponseFactory.ok(SuccessCode.REVIEW_HAS_BEEN_UPDATED);
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> removeRating(@PathVariable("id") Long movieId,
                                                          @CurrentUserId Long userId) {
        ratedCommandService.deleteRating(movieId, userId);
        return apiResponseFactory.ok(SuccessCode.REVIEW_HAS_BEEN_DELETED);
    }

    @GetMapping("/rating/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMovieRating(@PathVariable("id") Long movieId) {
        BigDecimal movieRating = ratedQueryService.getMovieRating(movieId);
        return ResponseEntity.ok(Map.of("rating", ratedQueryService.getNormalizedRating(movieRating)));
    }

    @GetMapping("/count/{movieId}")
    @ResponseBody
    public ResponseEntity<Long> getReviewsCount(@PathVariable Long movieId) {
        return ResponseEntity.ok(ratedQueryService.getReviewsCountForMovie(movieId));
    }
}