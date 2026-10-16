package com.example.demo.web.security;

import com.example.demo.dto.user.AppUserDetails;
import com.example.demo.dto.user.SessionUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class CurrentUserProvider {

    public SessionUser getSessionUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AppUserDetails appUserDetails)) {
            return SessionUser.ANONYMOUS;
        }
        return appUserDetails.sessionUser();
    }

    public boolean isAuthenticated() {
        return !SessionUser.ANONYMOUS.equals(getSessionUser());
    }
}