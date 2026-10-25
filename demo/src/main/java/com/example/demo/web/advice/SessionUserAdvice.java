package com.example.demo.web.advice;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.BadgeService;
import com.example.demo.web.security.CurrentUserProvider;
import lombok.AllArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@AllArgsConstructor
public class SessionUserAdvice {

    private final BadgeService badgeService;
    private final CurrentUserProvider currentUserProvider;

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        SessionUser sessionUser = currentUserProvider.getSessionUser();
        model.addAttribute("sessionUser", sessionUser);
        if (!sessionUser.isAuthenticated()) {
            model.addAttribute("badges", BadgeCountsDTO.builder().build());
            return;
        }
        model.addAttribute("badges", badgeService.getBadgeCounts(sessionUser.id()));
    }
}