package com.example.demo.web.util;

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

    public void authenticate(SessionUser sessionUser,
                             String rawPassword,
                             HttpServletRequest httpServletRequest,
                             HttpServletResponse httpServletResponse) {
        AppUserDetails appUserDetails = AppUserDetails.builder()
                .sessionUser(sessionUser)
                .passwordHash(null)
                .build();
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                appUserDetails,
                rawPassword,
                appUserDetails.getAuthorities()
        );
        saveAuthentication(authentication, httpServletRequest, httpServletResponse);
    }

    public void updatePrincipal(SessionUser updatedSessionUser,
                                HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse) {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        AppUserDetails currentPrincipal = (AppUserDetails) currentAuthentication.getPrincipal();
        AppUserDetails updatedPrincipal = AppUserDetails.builder()
                .sessionUser(updatedSessionUser)
                .passwordHash(currentPrincipal.getPasswordHash())
                .build();
        Authentication updatedAuthentication = new UsernamePasswordAuthenticationToken(
                updatedPrincipal,
                currentAuthentication.getCredentials(),
                updatedPrincipal.getAuthorities()
        );
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