package com.example.demo.controller;

import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.purchased.PurchasedCommandService;
import com.example.demo.util.MessageUtils;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class PurchasedController {

    private final PurchasedCommandService purchasedCommandService;
    private final MessageUtils messageUtils;

    @PostMapping("/add/bulk")
    @ResponseBody
    public ResponseEntity<ApiResponse> buyMovies(@SessionAttribute SessionUser sessionUser,
                                                 HttpSession httpSession) {
        Long userId = sessionUser.id();
        BigDecimal newBalance = purchasedCommandService.purchaseBulk(userId);
        SessionUser updated = sessionUser.withBalance(newBalance);
        httpSession.setAttribute("sessionUser", updated);
        String message = messageUtils.getSuccess(SuccessCode.PURCHASED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.PURCHASED.name())
                .message(message)
                .data(Map.of())
                .build());
    }

    @PostMapping("/add/{id}")
    @ResponseBody
    public ResponseEntity<ApiResponse> buyMovie(@PathVariable Long id,
                                                @SessionAttribute SessionUser sessionUser,
                                                HttpSession httpSession) {
        Long userId = sessionUser.id();
        BigDecimal newBalance = purchasedCommandService.purchase(id, userId);
        SessionUser updated = sessionUser.withBalance(newBalance);
        httpSession.setAttribute("sessionUser", updated);
        String message = messageUtils.getSuccess(SuccessCode.PURCHASED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.PURCHASED.name())
                .message(message)
                .data(Map.of())
                .build());
    }
}