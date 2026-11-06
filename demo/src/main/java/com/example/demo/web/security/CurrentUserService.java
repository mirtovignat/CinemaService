package com.example.demo.web.security;

import com.example.demo.dto.user.AppUserDetails;
import com.example.demo.dto.user.SessionUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;

@Component
@AllArgsConstructor
public class CurrentUserService {

    private final SecurityContextRepository securityContextRepository;

    public SessionUser get() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null
                || !(authentication.getPrincipal() instanceof AppUserDetails appUserDetails)) {
            return SessionUser.ANONYMOUS;
        }
        return appUserDetails.sessionUser();
    }

    public boolean isAuthenticated() {
        return !SessionUser.ANONYMOUS.equals(get());
    }

    public void updateUsername(String username) {
        apply(get().withUsername(username));
    }

    public void updateBalance(BigDecimal balance) {
        apply(get().withBalance(balance));
    }

    public void set(SessionUser sessionUser) {
        apply(sessionUser == null ? SessionUser.ANONYMOUS : sessionUser);
    }

    private void apply(SessionUser updatedSessionUser) {
        Authentication currentAuthentication = SecurityContextHolder.getContext().getAuthentication();
        if (currentAuthentication == null
                || !(currentAuthentication.getPrincipal() instanceof AppUserDetails currentAppUserDetails)) {
            return;
        }
        AppUserDetails updatedAppUserDetails = currentAppUserDetails.withSessionUser(updatedSessionUser);
        Authentication updatedAuthentication = new UsernamePasswordAuthenticationToken(
                updatedAppUserDetails, null, updatedAppUserDetails.getAuthorities()
        );
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(updatedAuthentication);
        SecurityContextHolder.setContext(securityContext);

        ServletRequestAttributes servletRequestAttributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (servletRequestAttributes == null) {
            return;
        }
        HttpServletRequest httpServletRequest = servletRequestAttributes.getRequest();
        HttpServletResponse httpServletResponse = servletRequestAttributes.getResponse();
        if (httpServletResponse == null) {
            return;
        }
        securityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);
    }
}