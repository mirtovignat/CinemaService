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
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserCommandService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.util.ReturnUrlHelper;
import com.example.demo.web.util.SessionAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
    private final FilterMetaService filterMetaService;
    private final SessionAuthenticator sessionAuthenticator;
    private final ReturnUrlHelper returnUrlHelper;

    @GetMapping("/personal-account")
    public String getPersonalAccount(SessionUser sessionUser, Model model) {
        Long userId = sessionUser.id();
        UserForOwnerViewDTO userForOwnerViewDTO = userQueryService.getUserForOwner(userId);
        PersonalAccountResponse response = userQueryService.buildUserPage(userForOwnerViewDTO);
        model.addAttribute("response", response);
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return "personal-account";
    }

    @GetMapping("/profile/edit")
    public String editProfile(SessionUser sessionUser, Model model) {
        Long userId = sessionUser.id();
        UserForOwnerViewDTO updatedUser = userQueryService.getUserForOwner(userId);
        PersonalAccountResponse response = userQueryService.buildUserPage(updatedUser);
        model.addAttribute("response", response);
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return "profile";
    }

    // Раньше был GET /settings -> "settings". Теперь сразу редирект на смену пароля.
    @GetMapping("/settings")
    public String getSettings() {
        return "redirect:/settings/change/pwd";
    }

    @PatchMapping("/settings/change/field")
    public ResponseEntity<ApiResponse> updateField(
            @Valid @RequestBody ProfileFieldUpdateDTO profileFieldUpdateDTO,
            SessionUser sessionUser,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        Long userId = sessionUser.id();
        ProfileFieldUpdateDTO validatedDto = userCommandService.prepareAndValidateField(profileFieldUpdateDTO, userId);
        userCommandService.updateField(userId, validatedDto);

        SessionUser updatedSessionUser = sessionUser;
        if (validatedDto.identifier() == Identifier.USERNAME) {
            updatedSessionUser = sessionUser.withUsername(validatedDto.field());
            sessionAuthenticator.updatePrincipal(updatedSessionUser, httpServletRequest, httpServletResponse);
        }
        UserForOwnerViewDTO updatedUser = userQueryService.getUserForOwner(userId);
        return respond(SuccessCode.UPDATED, Map.of(
                "user", updatedUser,
                "sessionUser", updatedSessionUser
        ));
    }

    @DeleteMapping("/settings/delete/phone")
    @ResponseBody
    public ResponseEntity<ApiResponse> deletePhone(SessionUser sessionUser) {
        userCommandService.deletePhone(sessionUser.id());
        return respond(SuccessCode.PHONE_HAS_BEEN_REMOVED, Map.of());
    }

    @GetMapping("/settings/change/pwd")
    public String getChangePasswordForm(
            HttpServletRequest httpServletRequest,
            HttpSession httpSession,
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
            SessionUser sessionUser,
            RedirectAttributes redirectAttributes,
            HttpSession httpSession
    ) {
        if (bindingResult.hasErrors()) {
            return "change-password";
        }
        Long userId = sessionUser.id();
        String newPasswordHash = userCommandService.prepareAndValidatePassword(passwordChangingDTO, userId);
        userCommandService.updatePassword(newPasswordHash, userId);
        String message = messageUtils.getSuccess(SuccessCode.PASSWORD_HAS_BEEN_CHANGED);
        redirectAttributes.addFlashAttribute("successMessage", message);
        String redirectUrl = returnUrlHelper.getReturnUrlOrDefault(httpSession, "/personal-account");
        return "redirect:" + redirectUrl;
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(
            SessionUser sessionUser,
            HttpSession httpSession,
            RedirectAttributes redirectAttributes
    ) {
        userCommandService.deleteAccount(sessionUser.id());
        httpSession.invalidate();
        String message = messageUtils.getSuccess(SuccessCode.ACCOUNT_HAS_BEEN_REMOVED);
        redirectAttributes.addFlashAttribute("successMessage", message);
        return "redirect:/login";
    }

    private ResponseEntity<ApiResponse> respond(SuccessCode successCode, Map<String, Object> data) {
        String message = messageUtils.getSuccess(successCode);
        return ResponseEntity.ok(ApiResponse.builder()
                .code(successCode.name())
                .message(message)
                .data(data)
                .build());
    }
}