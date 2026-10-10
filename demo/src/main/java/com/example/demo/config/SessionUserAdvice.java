package com.example.demo.config;

import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.user.AppUserDetails;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.BadgeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@RequiredArgsConstructor
public class SessionUserAdvice {

    private final BadgeService badgeService;

    @ModelAttribute("sessionUser")
    public SessionUser sessionUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AppUserDetails appUserDetails) {
            return appUserDetails.getSessionUser();
        }
        return null;
    }

    @ModelAttribute("badges")
    public BadgeCountsDTO badges() {
        SessionUser sessionUser = sessionUser();
        if (sessionUser == null) {
            return BadgeCountsDTO.builder().build();
        }
        return badgeService.getBadgeCounts(sessionUser.id());
    }

    @ModelAttribute("cartCount")
    public Long cartCount() {
        Long count = badges().cartCount();
        return count != null ? count : 0L;
    }

    @ModelAttribute("favouritesCount")
    public Long favouritesCount() {
        Long count = badges().favouritesCount();
        return count != null ? count : 0L;
    }
}