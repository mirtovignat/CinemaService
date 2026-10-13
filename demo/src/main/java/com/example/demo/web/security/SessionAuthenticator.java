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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SessionAuthenticator {

    private final SecurityContextRepository securityContextRepository;
    private final UserDetailsService userDetailsService;

    public void authenticate(String username,
                             HttpServletRequest httpServletRequest,
                             HttpServletResponse httpServletResponse) {
        UserDetails loaded = userDetailsService.loadUserByUsername(username);
        if (!(loaded instanceof AppUserDetails userDetails)) {
            throw new IllegalStateException(
                    "UserDetailsService must return AppUserDetails, got: "
                            + loaded.getClass().getName());
        }
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        saveAuthentication(authentication, httpServletRequest, httpServletResponse);
    }

    public void updateSessionUser(SessionUser updatedSessionUser,
                                  HttpServletRequest httpServletRequest,
                                  HttpServletResponse httpServletResponse) {
        Authentication current = SecurityContextHolder.getContext().getAuthentication();
        if (current == null || !(current.getPrincipal() instanceof AppUserDetails currentPrincipal)) {
            throw new IllegalStateException("No authenticated AppUserDetails in SecurityContext");
        }
        AppUserDetails updatedPrincipal = currentPrincipal.withSessionUser(updatedSessionUser);
        Authentication updated = new UsernamePasswordAuthenticationToken(
                updatedPrincipal, null, updatedPrincipal.getAuthorities());
        saveAuthentication(updated, httpServletRequest, httpServletResponse);
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