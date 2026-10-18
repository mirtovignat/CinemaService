package com.example.demo.controller;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.catalog.ReviewViewDTO;
import com.example.demo.dto.catalog.ReviewsQueryDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.CursorPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.cursor.ReviewCursorService;
import com.example.demo.service.cursor.util.ReviewCursorContext;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.rated.RatedCommandService;
import com.example.demo.service.rated.RatedQueryService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
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
                             SessionUser sessionUser,
                             @CookieValue(value = ReviewSortResolver.SORT_COOKIE_NAME, required = false) String sortCookie,
                             HttpServletResponse httpServletResponse,
                             Model model) {
        Long userId = sessionUser.idOrNull();

        ReviewsQueryDTO.SortDirection sortDirection =
                reviewSortResolver.resolveAndPersist(reviewsQueryDTO, sortCookie, httpServletResponse);

        CursorPageResponse<ReviewViewDTO> page =
                reviewCursorService.page(reviewsQueryDTO.cursor(), sortDirection.name(),
                        new ReviewCursorContext(id, userId));

        model.addAttribute("response", page);
        model.addAttribute("ownReview", userId != null
                ? ratedQueryService.getRating(id, userId)
                : null);
        model.addAttribute("movieId", id);
        model.addAttribute("movieTitle", movieService.getMovie(id).getTitle());
        model.addAttribute("avgRating", ratedQueryService.getMovieRating(id));
        model.addAttribute("totalReviews", ratedQueryService.getReviewsCountForMovie(id));
        if (userId != null) {
            model.addAttribute("currentUsername", userQueryService.getUsername(userId));
        }
        return "catalog/reviews";
    }

    @GetMapping("/{id}/reviews/more")
    @ResponseBody
    public CursorPageResponse<ReviewViewDTO> getReviewsMore(@PathVariable Long id,
                                                            @ModelAttribute ReviewsQueryDTO reviewsQueryDTO,
                                                            SessionUser sessionUser,
                                                            @CookieValue(value = ReviewSortResolver.SORT_COOKIE_NAME, required = false) String sortCookie) {
        Long userId = sessionUser.idOrNull();

        ReviewsQueryDTO.SortDirection sortDirection =
                reviewSortResolver.resolve(reviewsQueryDTO, sortCookie);

        return reviewCursorService.page(reviewsQueryDTO.cursor(), sortDirection.name(),
                new ReviewCursorContext(id, userId));
    }

    @PostMapping("/{id}/reviews/delete")
    public String deleteReview(@PathVariable("id") Long movieId,
                               SessionUser sessionUser,
                               RedirectAttributes redirectAttributes) {
        Long userId = sessionUser.id();
        ratedCommandService.deleteRating(movieId, userId);
        String message = messageUtils.getSuccess(SuccessCode.REVIEW_HAS_BEEN_DELETED);
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/rated/" + movieId + "/reviews";
    }

    @GetMapping("/form/{movieId}")
    @ResponseBody
    public ResponseEntity<RatedMovieFormDTO> getMyReviewForm(@PathVariable Long movieId,
                                                             SessionUser sessionUser) {
        Long userId = sessionUser.id();
        return ResponseEntity.ok(ratedQueryService.getPreFilledForm(movieId, userId));
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<ApiResponse> addRating(@Valid @ModelAttribute RatedMovieFormDTO ratedMovieFormDTO,
                                                 SessionUser sessionUser) {
        Long userId = sessionUser.id();
        ratedCommandService.addOrUpdateRating(ratedMovieFormDTO.id(), userId, ratedMovieFormDTO);
        return apiResponseFactory.ok(SuccessCode.REVIEW_HAS_BEEN_SAVED);
    }

    @PostMapping("/edit")
    @ResponseBody
    public ResponseEntity<ApiResponse> editRating(@Valid @ModelAttribute RatedMovieFormDTO ratedMovieFormDTO,
                                                  SessionUser sessionUser) {
        Long userId = sessionUser.id();
        ratedCommandService.addOrUpdateRating(ratedMovieFormDTO.id(), userId, ratedMovieFormDTO);
        return apiResponseFactory.ok(SuccessCode.REVIEW_HAS_BEEN_UPDATED);
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> removeRating(@PathVariable("id") Long movieId,
                                                    SessionUser sessionUser) {
        Long userId = sessionUser.id();
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