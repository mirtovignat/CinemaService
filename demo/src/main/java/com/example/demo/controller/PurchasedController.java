package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PurchasedMovieResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.catalog.PurchasedCommandService;
import com.example.demo.web.security.SessionAuthenticator;
import com.example.demo.web.util.ApiResponseFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

    private final PurchasedCommandService purchasedCommandService;
    private final SessionAuthenticator sessionAuthenticator;
    private final ApiResponseFactory apiResponseFactory;

    @PostMapping("/add/bulk")
    public ResponseEntity<ApiResponse<PurchasedMovieResponse>> buyMovies(
            SessionUser sessionUser,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        BigDecimal newBalance = purchasedCommandService.purchaseBulk(sessionUser.id());
        return respond(sessionUser, newBalance, httpServletRequest, httpServletResponse);
    }

    @PostMapping("/add/{id}")
    public ResponseEntity<ApiResponse<PurchasedMovieResponse>> buyMovie(
            @PathVariable Long id,
            SessionUser sessionUser,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) {
        BigDecimal newBalance = purchasedCommandService.purchase(
                id,
                sessionUser.id()
        );
        return respond(
                sessionUser,
                newBalance,
                httpServletRequest,
                httpServletResponse
        );
    }

    private ResponseEntity<ApiResponse<PurchasedMovieResponse>> respond(SessionUser sessionUser,
                                                                        BigDecimal newBalance,
                                                                        HttpServletRequest httpServletRequest,
                                                                        HttpServletResponse httpServletResponse) {
        sessionAuthenticator.updateSessionUser(
                sessionUser.withBalance(newBalance),
                httpServletRequest,
                httpServletResponse
        );
        return apiResponseFactory.ok(SuccessCode.PURCHASED, PurchasedMovieResponse.builder()
                .newBalance(newBalance)
                .build());
    }
}