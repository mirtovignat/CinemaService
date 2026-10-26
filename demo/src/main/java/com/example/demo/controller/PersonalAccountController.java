package com.example.demo.controller;

import com.example.demo.dto.authorize.Identifier;
import com.example.demo.dto.response.ApiResponse;
import com.example.demo.dto.response.PersonalAccountResponse;
import com.example.demo.dto.response.UpdateFieldResponse;
import com.example.demo.dto.user.PasswordChangingDTO;
import com.example.demo.dto.user.ProfileFieldUpdateDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.dto.user.UserForOwnerViewDTO;
import com.example.demo.exception.SuccessCode;
import com.example.demo.model.entity.User;
import com.example.demo.service.movie.MovieFilterMetaService;
import com.example.demo.service.user.UserCommandService;
import com.example.demo.service.user.UserQueryService;
import com.example.demo.util.MessageUtils;
import com.example.demo.web.resolver.CurrentUserId;
import com.example.demo.web.security.SessionAuthenticator;
import com.example.demo.web.util.ApiResponseFactory;
import com.example.demo.web.util.NavigationUtils;
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

@Controller
@AllArgsConstructor
public class PersonalAccountController {

    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;
    private final MessageUtils messageUtils;
    private final MovieFilterMetaService movieFilterMetaService;
    private final SessionAuthenticator sessionAuthenticator;
    private final ApiResponseFactory apiResponseFactory;

    @GetMapping("/personal-account")
    public String getPersonalAccount(@CurrentUserId Long userId, Model model) {
        User user = userQueryService.getUser(userId);
        PersonalAccountResponse personalAccountResponse = userQueryService.buildUserPage(user);
        model.addAttribute("personalAccountResponse", personalAccountResponse);
        model.addAttribute("genres", movieFilterMetaService.getAllGenres());
        return "account/personal-account";
    }

    @GetMapping("/profile/edit")
    public String editProfile(@CurrentUserId Long userId, Model model) {
        User user = userQueryService.getUser(userId);
        PersonalAccountResponse personalAccountResponse = userQueryService.buildUserPage(user);
        model.addAttribute("personalAccountResponse", personalAccountResponse);
        model.addAttribute("genres", movieFilterMetaService.getAllGenres());
        return "account/profile";
    }

    @GetMapping("/settings")
    public String getSettings() {
        return "redirect:/settings/change/pwd";
    }

    @PatchMapping("/settings/change/field")
    @ResponseBody
    public ResponseEntity<ApiResponse<UpdateFieldResponse>> updateField(
            @Valid @RequestBody ProfileFieldUpdateDTO profileFieldUpdateDTO,
            SessionUser sessionUser,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse
    ) {
        Long userId = sessionUser.id();
        ProfileFieldUpdateDTO validatedProfileFieldUpdateDTO =
                userCommandService.prepareAndValidateField(profileFieldUpdateDTO, userId);
        userCommandService.updateField(userId, validatedProfileFieldUpdateDTO);

        SessionUser updatedSessionUser = sessionUser;
        if (validatedProfileFieldUpdateDTO.identifier() == Identifier.USERNAME) {
            updatedSessionUser = sessionUser.withUsername(validatedProfileFieldUpdateDTO.field());
            sessionAuthenticator.updateSessionUser(updatedSessionUser, httpServletRequest, httpServletResponse);
        }
        UserForOwnerViewDTO userForOwnerViewDTO = userQueryService.getUserForOwner(userId);
        UpdateFieldResponse updateFieldResponse = UpdateFieldResponse.builder()
                .userForOwnerViewDTO(userForOwnerViewDTO)
                .sessionUser(updatedSessionUser)
                .build();
        return apiResponseFactory.ok(SuccessCode.UPDATED, updateFieldResponse);
    }

    @DeleteMapping("/settings/delete/phone")
    @ResponseBody
    public ResponseEntity<ApiResponse<Void>> deletePhone(@CurrentUserId Long userId) {
        userCommandService.deletePhone(userId);
        return apiResponseFactory.ok(SuccessCode.PHONE_HAS_BEEN_REMOVED);
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
            @CurrentUserId Long userId,
            HttpSession httpSession,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "account/change-password";
        }
        String newPasswordHash = userCommandService.prepareAndValidatePassword(passwordChangingDTO, userId);
        userCommandService.updatePassword(newPasswordHash, userId);
        String successMessage = messageUtils.getSuccess(SuccessCode.PASSWORD_HAS_BEEN_CHANGED);
        redirectAttributes.addFlashAttribute("successMessage", successMessage);

        String targetPath = NavigationUtils.resolvePreviousPage(httpSession, "/personal-account");
        return "redirect:" + targetPath;
    }

    @PostMapping("/profile/delete")
    public String deleteAccount(
            @CurrentUserId Long userId,
            HttpSession httpSession,
            RedirectAttributes redirectAttributes
    ) {
        userCommandService.deleteAccount(userId);
        httpSession.invalidate();
        String successMessage = messageUtils.getSuccess(SuccessCode.ACCOUNT_HAS_BEEN_REMOVED);
        redirectAttributes.addFlashAttribute("successMessage", successMessage);
        return "redirect:/login";
    }
}