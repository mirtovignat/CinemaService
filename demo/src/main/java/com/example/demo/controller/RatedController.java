package com.example.demo.controller;

import com.example.demo.dto.catalog.RatedMovieFormDTO;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.ReviewsPageResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ErrorCode;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.rated.RatedCommandService;
import com.example.demo.service.rated.RatedQueryService;
import com.example.demo.service.rated.ReviewsQueryService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequestMapping("/rated")
@RequiredArgsConstructor
public class RatedController {

    private final RatedCommandService ratedCommandService;
    private final RatedQueryService ratedQueryService;
    private final ReviewsQueryService reviewsPageService;
    private final UserQueryService userQueryService;
    private final MessageUtils messageUtils;

    @GetMapping("/{id}/reviews")
    public String getReviews(
            @PathVariable Long id,
            @SessionAttribute(required = false) SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser != null ? sessionUser.id() : null;
        ReviewsPageResponse response = reviewsPageService.buildReviewsPage(id, userId);
        model.addAttribute("response", response);
        if (userId != null) {
            String username = userQueryService.getUsername(userId);
            model.addAttribute("currentUsername", username);
        }
        return "reviews";
    }

    @PostMapping("/{id}/reviews/delete")
    public String deleteReview(@PathVariable("id") Long movieId,
                               @SessionAttribute SessionUser sessionUser,
                               RedirectAttributes redirectAttributes) {
        Long userId = sessionUser.id();
        ratedCommandService.deleteRating(movieId, userId);
        String message = messageUtils.getSuccess(SuccessCode.REVIEW_HAS_BEEN_DELETED);
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/rated/" + movieId + "/reviews";
    }

    @GetMapping("/form/{movieId}")
    @ResponseBody
    public ResponseEntity<RatedMovieFormDTO> getMyReviewForm(
            @PathVariable Long movieId,
            @SessionAttribute SessionUser sessionUser
    ) {
        Long userId = sessionUser.id();
        return ResponseEntity.ok(ratedQueryService.getPreFilledForm(movieId, userId));
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<ApiResponse> addRating(@Valid @ModelAttribute RatedMovieFormDTO dto,
                                                 @SessionAttribute SessionUser sessionUser) {
        Long userId = sessionUser.id();

        if (ratedQueryService.isUnchanged(dto.id(), userId, dto)) {
            throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
        }

        ratedCommandService.addOrUpdateRating(dto.id(), userId, dto);
        String message = messageUtils.getSuccess(SuccessCode.REVIEW_HAS_BEEN_SAVED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.REVIEW_HAS_BEEN_SAVED.name())
                .message(message)
                .data(Map.of())
                .build());
    }

    @PostMapping("/edit")
    @ResponseBody
    public ResponseEntity<ApiResponse> editRating(@Valid @ModelAttribute RatedMovieFormDTO dto,
                                                  @SessionAttribute SessionUser sessionUser) {
        Long userId = sessionUser.id();

        if (ratedQueryService.isUnchanged(dto.id(), userId, dto)) {
            throw BusinessException.of(ErrorCode.DATA_COINCIDENCE);
        }

        ratedCommandService.addOrUpdateRating(dto.id(), userId, dto);
        String message = messageUtils.getSuccess(SuccessCode.REVIEW_HAS_BEEN_UPDATED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.REVIEW_HAS_BEEN_UPDATED.name())
                .message(message)
                .data(Map.of())
                .build());
    }

    @PostMapping("/remove/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> removeRating(@PathVariable("id") Long movieId,
                                                    @SessionAttribute SessionUser sessionUser) {
        Long userId = sessionUser.id();
        ratedCommandService.deleteRating(movieId, userId);
        String message = messageUtils.getSuccess(SuccessCode.REVIEW_HAS_BEEN_DELETED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.REVIEW_HAS_BEEN_DELETED.name())
                .message(message)
                .data(Map.of())
                .build());
    }

    @GetMapping("/rating/{id}")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getMovieRating(@PathVariable("id") Long movieId) {
        BigDecimal rating = ratedQueryService.getMovieRating(movieId);
        return ResponseEntity.ok(Map.of("rating", ratedQueryService.getNormalizedRating(rating)));
    }

    @GetMapping("/count/{movieId}")
    @ResponseBody
    public ResponseEntity<Long> getReviewsCount(@PathVariable Long movieId) {
        return ResponseEntity.ok(ratedQueryService.getReviewsCountForMovie(movieId));
    }
}