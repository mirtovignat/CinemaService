package com.example.demo.web.security;

import com.example.demo.dto.user.AppUserDetails;
import com.example.demo.dto.user.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAuthenticator {

    private final SecurityContextRepository securityContextRepository;

    public void updateSessionUser(SessionUser updatedSessionUser,
                                  HttpServletRequest httpServletRequest,
                                  HttpServletResponse httpServletResponse) {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuthentication == null
                || !(currentAuthentication.getPrincipal() instanceof AppUserDetails currentAppUserDetails)) {
            throw new IllegalStateException("No authenticated AppUserDetails in SecurityContext");
        }
        AppUserDetails updatedAppUserDetails = currentAppUserDetails.withSessionUser(updatedSessionUser);
        Authentication updatedAuthentication = new UsernamePasswordAuthenticationToken(
                updatedAppUserDetails, null, updatedAppUserDetails.getAuthorities());
        saveAuthentication(updatedAuthentication, httpServletRequest, httpServletResponse);
    }

    private void saveAuthentication(Authentication authentication,
                                    HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse) {
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);
    }
}