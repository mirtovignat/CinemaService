package com.example.demo.controller;

import com.example.demo.dto.authorize.Identifier;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PersonalAccountResponse;
import com.example.demo.dto.user.PasswordChangingDTO;
import com.example.demo.dto.user.ProfileFieldUpdateDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.exception.SuccessCode;
import com.example.demo.service.catalog.WatchedHistoryService;
import com.example.demo.service.movie.MovieService;
import com.example.demo.service.user.UserCommandService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.ReturnUrlHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequiredArgsConstructor
public class PersonalAccountController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final MessageUtils messageUtils;
    private final WatchedHistoryService watchHistoryService;
    private final MovieService movieService;

    @GetMapping("/personal-account")
    public String getPersonalAccount(
            @SessionAttribute SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser.id();
        UserForOwnerViewDTO userForOwnerViewDTO = userQueryService.getUserForOwner(userId);
        PersonalAccountResponse response = userQueryService.buildUserPage(userForOwnerViewDTO);
        model.addAttribute("response", response);
        model.addAttribute("genres", movieService.getAllGenres());
        return "personal-account";
    }

    @GetMapping("/profile/edit")
    public String editProfile(
            @SessionAttribute SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser.id();
        UserForOwnerViewDTO updatedUser = userQueryService.getUserForOwner(userId);
        PersonalAccountResponse response = userQueryService.buildUserPage(updatedUser);
        model.addAttribute("response", response);
        return "profile";
    }

    @GetMapping("/settings")
    public String getSettings(
            @SessionAttribute SessionUser sessionUser,
            Model model
    ) {
        Long userId = sessionUser.id();
        model.addAttribute("saveHistory", watchHistoryService.isHistorySaved(userId));
        return "settings";
    }

    @PatchMapping("/settings/change/field")
    public ResponseEntity<ApiResponse> updateField(
            @Valid @RequestBody ProfileFieldUpdateDTO profileFieldUpdateDTO,
            @SessionAttribute SessionUser sessionUser,
            HttpSession httpSession
    ) {
        Long userId = sessionUser.id();
        ProfileFieldUpdateDTO validatedDto = userCommandService.prepareAndValidateField(profileFieldUpdateDTO, userId);
        userCommandService.updateField(userId, validatedDto);
        SessionUser updatedSessionUser = sessionUser;
        if (validatedDto.identifier() == Identifier.USERNAME) {
            updatedSessionUser = sessionUser.withUsername(validatedDto.field());
            httpSession.setAttribute("sessionUser", updatedSessionUser);
        }
        UserForOwnerViewDTO updatedUser = userQueryService.getUserForOwner(userId);
        String message = messageUtils.getSuccess(SuccessCode.UPDATED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.UPDATED.name())
                .message(message)
                .data(Map.of(
                        "user", updatedUser,
                        "sessionUser", updatedSessionUser
                ))
                .build());
    }

    @GetMapping("/settings/delete/phone")
    @ResponseBody
    public ResponseEntity<ApiResponse> deletePhone(@SessionAttribute SessionUser sessionUser) {
        Long userId = sessionUser.id();
        userCommandService.deletePhone(userId);
        String message = messageUtils.getSuccess(SuccessCode.PHONE_HAS_BEEN_REMOVED);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(SuccessCode.PHONE_HAS_BEEN_REMOVED.name())
                .message(message)
                .data(Map.of())
                .build());
    }

    @GetMapping("/settings/change/pwd")
    public String getChangePasswordForm(
            HttpServletRequest httpServletRequest,
            HttpSession httpSession,
            ReturnUrlHelper returnUrlHelper,
            Model model
    ) {
        returnUrlHelper.saveReturnUrlIfNotExists(httpSession, httpServletRequest, "/settings/change/pwd");
        if (!model.containsAttribute("passwordChangingDTO")) {
            model.addAttribute("passwordChangingDTO", PasswordChangingDTO.builder().build());
        }
        return "change-password";
    }

    @PostMapping("/settings/change/pwd")
    public String changePassword(
            @Valid @ModelAttribute("passwordChangingDTO") PasswordChangingDTO passwordChangingDTO,
            BindingResult bindingResult,
            @SessionAttribute SessionUser sessionUser,
            RedirectAttributes redirectAttributes,
            HttpSession httpSession,
            ReturnUrlHelper returnUrlHelper
    ) {
        Long userId = sessionUser.id();
        if (bindingResult.hasErrors()) {
            return "change-password";
        }
        String newPasswordHash = userCommandService.prepareAndValidatePassword(passwordChangingDTO, userId);
        userCommandService.updatePassword(newPasswordHash, userId);
        String message = messageUtils.getSuccess(SuccessCode.PASSWORD_HAS_BEEN_CHANGED);
        redirectAttributes.addFlashAttribute("successMessage", message);
        String redirectUrl = returnUrlHelper.getReturnUrlOrDefault(httpSession, "/personal-account");
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(
            @SessionAttribute SessionUser sessionUser,
            HttpSession httpSession,
            RedirectAttributes redirectAttributes
    ) {
        Long userId = sessionUser.id();
        userCommandService.deleteAccount(userId);
        httpSession.invalidate();
        String message = messageUtils.getSuccess(SuccessCode.ACCOUNT_HAS_BEEN_REMOVED);
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/login";
    }
}