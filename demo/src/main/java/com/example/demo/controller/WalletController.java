package com.example.demo.controller;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.dto.wallet.TopUpFormDTO;
import com.example.demo.dto.wallet.WalletForOwnerViewDTO;
import com.example.demo.exception.SuccessCode;
import com.example.demo.model.entity.User;
import com.example.demo.service.user.UserCommandService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.ReturnUrlHelper;
import com.example.demo.web.util.SessionAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
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
    public String getTopUpForm(HttpServletRequest httpServletRequest,
                               HttpSession httpSession,
                               ReturnUrlHelper returnUrlHelper,
                               SessionUser sessionUser,
                               Model model) {
        returnUrlHelper.saveReturnUrlIfNotExists(httpSession, httpServletRequest, "/wallet/top-up");
        Long userId = sessionUser.id();
        if (!model.containsAttribute("topUpFormDTO")) {
            model.addAttribute("topUpFormDTO", TopUpFormDTO.builder().build());
        }
        model.addAttribute("wallet", userQueryService.getWalletForOwner(userId));
        return "top-up";
    }

    @GetMapping("/balance")
    @ResponseBody
    public ResponseEntity<WalletForOwnerViewDTO> balance(
            SessionUser sessionUser) {
        Long userId = sessionUser.id();
        return ResponseEntity.ok(userQueryService.getWalletForOwner(userId));
    }

    @PostMapping("/top-up")
    public String topUp(@Valid @ModelAttribute("topUpFormDTO") TopUpFormDTO topUpFormDTO,
                        BindingResult bindingResult,
                        SessionUser sessionUser,
                        HttpServletRequest httpServletRequest,
                        HttpServletResponse httpServletResponse,
                        HttpSession httpSession,
                        RedirectAttributes redirectAttributes,
                        ReturnUrlHelper returnUrlHelper,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("wallet", userQueryService.getWalletForOwner(sessionUser.id()));
            return "top-up";
        }
        User user = userQueryService.getUser(sessionUser.id());
        userCommandService.saveTopUp(topUpFormDTO.amount(), user);
        BigDecimal newBalance = userQueryService.getBalance(sessionUser.id());
        sessionAuthenticator.updatePrincipal(
                sessionUser.withBalance(newBalance),
                httpServletRequest,
                httpServletResponse
        );
        String message = messageUtils.getSuccess(SuccessCode.TOPPED_UP);
        redirectAttributes.addFlashAttribute("successMessage", message);
        String redirectUrl = returnUrlHelper.getReturnUrlOrDefault(httpSession, "/personal-account");
        return "redirect:" + redirectUrl;
    }
}