package com.example.demo.controller;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.dto.wallet.TopUpFormDTO;
import com.example.demo.exception.SuccessCode;
import com.example.demo.model.entity.User;
import com.example.demo.service.user.UserCommandService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.ReturnUrlHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
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

    @GetMapping("/top-up")
    public String getTopUpForm(HttpServletRequest request,
                               HttpSession session,
                               ReturnUrlHelper returnUrlHelper,
                               @SessionAttribute SessionUser sessionUser,
                               Model model) {
        returnUrlHelper.saveReturnUrlIfNotExists(session, request, "/wallet/top-up");
        Long userId = sessionUser.id();
        if (!model.containsAttribute("topUpFormDTO")) {
            model.addAttribute("topUpFormDTO", TopUpFormDTO.builder().build());
        }
        model.addAttribute("wallet", userQueryService.getWalletForOwner(userId));
        return "top-up";
    }

    @PostMapping("/top-up")
    public String topUp(@Valid @ModelAttribute("topUpFormDTO") TopUpFormDTO dto,
                        BindingResult bindingResult,
                        @SessionAttribute SessionUser sessionUser,
                        HttpSession httpSession,
                        RedirectAttributes redirectAttributes,
                        ReturnUrlHelper returnUrlHelper,
                        Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("wallet", userQueryService.getWalletForOwner(sessionUser.id()));
            return "top-up";
        }
        User user = userQueryService.getUser(sessionUser.id());
        userCommandService.saveTopUp(dto.amount(), user);
        BigDecimal newBalance = userQueryService.getBalance(sessionUser.id());
        SessionUser updated = sessionUser.withBalance(newBalance);
        httpSession.setAttribute("sessionUser", updated);
        String message = messageUtils.getSuccess(SuccessCode.TOPPED_UP);
        redirectAttributes.addFlashAttribute("successMessage", message);
        String redirectUrl = returnUrlHelper.getReturnUrlOrDefault(httpSession, "/personal-account");
        return "redirect:" + redirectUrl;
    }
}