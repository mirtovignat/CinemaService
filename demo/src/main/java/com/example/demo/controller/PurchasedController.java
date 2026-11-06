package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PurchasedMovieResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.catalog.PurchasedService;
import com.example.demo.web.security.CurrentUserService;
import com.example.demo.web.util.ApiResponseFactory;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.math.BigDecimal;

@Controller
@RequestMapping("/orders")
@AllArgsConstructor
public class PurchasedController {

    private final PurchasedService purchasedService;
    private final CurrentUserService currentUserService;
    private final ApiResponseFactory apiResponseFactory;

    @PostMapping("/add/bulk")
    public ResponseEntity<ApiResponse<PurchasedMovieResponse>> buyMovies(SessionUser sessionUser) {
        BigDecimal newBalance = purchasedService.purchaseBulk(sessionUser.id());
        currentUserService.updateBalance(newBalance);
        return apiResponseFactory.ok(SuccessCode.PURCHASED, PurchasedMovieResponse.of(newBalance));
    }

    @PostMapping("/add/{id}")
    public ResponseEntity<ApiResponse<PurchasedMovieResponse>> buyMovie(
            @PathVariable Long id,
            SessionUser sessionUser
    ) {
        BigDecimal newBalance = purchasedService.purchase(id, sessionUser.id());
        currentUserService.updateBalance(newBalance);
        return apiResponseFactory.ok(SuccessCode.PURCHASED, PurchasedMovieResponse.of(newBalance));
    }
}