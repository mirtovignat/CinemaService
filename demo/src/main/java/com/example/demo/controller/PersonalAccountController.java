package com.example.demo.controller;

import com.example.demo.dto.authorize.Identifier;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PersonalAccountResponse;
import com.example.demo.dto.user.PasswordChangingDTO;
import com.example.demo.dto.user.ProfileFieldUpdateDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.exception.SuccessCode;
import com.example.demo.model.entity.User;
import com.example.demo.service.movie.FilterMetaService;
import com.example.demo.service.user.UserCommandService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.interceptor.NavigationHistoryInterceptor;
import com.example.demo.web.security.SessionAuthenticator;
import com.example.demo.web.util.ApiResponseFactory;
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
    private final FilterMetaService filterMetaService;
    private final SessionAuthenticator sessionAuthenticator;
    private final ApiResponseFactory apiResponseFactory;

    @GetMapping("/personal-account")
    public String getPersonalAccount(SessionUser sessionUser, Model model) {
        Long userId = sessionUser.id();
        User user = userQueryService.getUser(userId);
        PersonalAccountResponse response = userQueryService.buildUserPage(user);
        model.addAttribute("response", response);
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return "account/personal-account";
    }

    @GetMapping("/profile/edit")
    public String editProfile(SessionUser sessionUser, Model model) {
        Long userId = sessionUser.id();
        User user = userQueryService.getUser(userId);
        PersonalAccountResponse response = userQueryService.buildUserPage(user);
        model.addAttribute("response", response);
        model.addAttribute("genres", filterMetaService.getAllGenres());
        return "account/profile";
    }

    @GetMapping("/settings")
    public String getSettings() {
        return "redirect:/settings/change/pwd";
    }

    @PatchMapping("/settings/change/field")
    @ResponseBody
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
            sessionAuthenticator.updateSessionUser(updatedSessionUser, httpServletRequest, httpServletResponse);
        }
        UserForOwnerViewDTO updatedUser = userQueryService.getUserForOwner(userId);
        return apiResponseFactory.ok(SuccessCode.UPDATED, Map.of(
                "user", updatedUser,
                "sessionUser", updatedSessionUser
        ));
    }

    @DeleteMapping("/settings/delete/phone")
    @ResponseBody
    public ResponseEntity<ApiResponse> deletePhone(SessionUser sessionUser) {
        userCommandService.deletePhone(sessionUser.id());
        return apiResponseFactory.ok(SuccessCode.PHONE_HAS_BEEN_REMOVED, Map.of());
    }

    @GetMapping("/settings/change/pwd")
    public String getChangePasswordForm(Model model) {
        if (!model.containsAttribute("passwordChangingDTO")) {
            model.addAttribute("passwordChangingDTO", PasswordChangingDTO.builder().build());
        }
        return "account/change-password";
    }

    @PostMapping("/settings/change/pwd")
    public String changePassword(
            @Valid @ModelAttribute("passwordChangingDTO") PasswordChangingDTO passwordChangingDTO,
            BindingResult bindingResult,
            SessionUser sessionUser,
            HttpSession httpSession,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }
        Long userId = sessionUser.id();
        String newPasswordHash = userCommandService.prepareAndValidatePassword(passwordChangingDTO, userId);
        userCommandService.updatePassword(newPasswordHash, userId);
        String message = messageUtils.getSuccess(SuccessCode.PASSWORD_HAS_BEEN_CHANGED);
        redirectAttributes.addFlashAttribute("successMessage", message);

        String target = resolveReturnPath(httpSession);
        return "redirect:" + target;
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

    private String resolveReturnPath(HttpSession httpSession) {
        if (httpSession == null) return "/personal-account";

        Object stored = httpSession.getAttribute(NavigationHistoryInterceptor.PREVIOUS_PAGE_ATTR);
        httpSession.removeAttribute(NavigationHistoryInterceptor.PREVIOUS_PAGE_ATTR);

        if (stored instanceof String storedPath && isSafeLocalPath(storedPath)) {
            return storedPath;
        }
        return "/personal-account";
    }

    private boolean isSafeLocalPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        if (!path.startsWith("/")) {
            return false;
        }
        if (path.startsWith("//")) {
            return false;
        }
        return !path.startsWith("/\\");
    }
}