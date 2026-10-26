package com.example.demo.controller;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.dto.wallet.TopUpFormDTO;
import com.example.demo.dto.wallet.WalletForOwnerViewDTO;
import com.example.demo.exception.SuccessCode;
import com.example.demo.model.entity.User;
import com.example.demo.service.user.UserCommandService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.security.SessionAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/wallet")
@AllArgsConstructor
public class WalletController {

    private final UserQueryService userQueryService;
    private final UserCommandService userCommandService;
    private final MessageUtils messageUtils;
    private final SessionAuthenticator sessionAuthenticator;

    @GetMapping("/top-up")
    public String getTopUpForm(@CurrentUserId Long userId, Model model) {
        if (!model.containsAttribute("topUpFormDTO")) {
            model.addAttribute("topUpFormDTO", TopUpFormDTO.builder().build());
        }
        model.addAttribute("wallet", userQueryService.getWalletForOwner(userId));
        return "account/top-up";
    }

    @GetMapping("/balance")
    @ResponseBody
    public ResponseEntity<WalletForOwnerViewDTO> balance(@CurrentUserId Long userId) {
        return ResponseEntity.ok(userQueryService.getWalletForOwner(userId));
    }

    @PostMapping("/top-up")
    public String topUp(@Valid @ModelAttribute("topUpFormDTO") TopUpFormDTO topUpFormDTO,
                        BindingResult bindingResult,
                        SessionUser sessionUser,
                        HttpServletRequest httpServletRequest,
                        HttpServletResponse httpServletResponse,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        Long userId = sessionUser.id();
        if (bindingResult.hasErrors()) {
            model.addAttribute("wallet", userQueryService.getWalletForOwner(userId));
            return "account/top-up";
        }
        User user = userQueryService.getUser(userId);
        userCommandService.saveTopUp(topUpFormDTO.amount(), user);
        BigDecimal newBalance = userQueryService.getBalance(userId);
        sessionAuthenticator.updateSessionUser(
                sessionUser.withBalance(newBalance),
                httpServletRequest,
                httpServletResponse
        );
        String message = messageUtils.getSuccess(SuccessCode.TOPPED_UP);
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/personal-account";
    }
}