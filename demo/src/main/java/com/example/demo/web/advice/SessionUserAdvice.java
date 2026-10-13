package com.example.demo.web.advice;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.BadgeService;
import com.example.demo.web.security.CurrentUserProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Optional;

@ControllerAdvice
@RequiredArgsConstructor
public class SessionUserAdvice {

    private final BadgeService badgeService;
    private final CurrentUserProvider currentUserProvider;

    @ModelAttribute
    public void addCommonAttributes(Model model) {
        Optional<SessionUser> sessionUser = currentUserProvider.getOptional();
        model.addAttribute("sessionUser", sessionUser.orElse(null));

        if (sessionUser.isEmpty()) {
            model.addAttribute("badges", BadgeCountsDTO.builder().build());
            model.addAttribute("cartCount", 0L);
            model.addAttribute("favouritesCount", 0L);
            return;
        }

        BadgeCountsDTO badgeCountsDTO = badgeService.getBadgeCounts(sessionUser.get().id());
        model.addAttribute("badges", badgeCountsDTO);
        model.addAttribute("cartCount", badgeCountsDTO.cartCount() != null ? badgeCountsDTO.cartCount() : 0L);
        model.addAttribute("favouritesCount", badgeCountsDTO.favouritesCount() != null ? badgeCountsDTO.favouritesCount() : 0L);
    }
}