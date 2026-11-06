package com.example.demo.web;

import com.example.demo.dto.user.SessionUser;
import com.example.demo.web.security.CurrentUserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
@AllArgsConstructor
public class SessionUserAdvice {

    private final CurrentUserService currentUserService;

    @ModelAttribute("sessionUser")
    public SessionUser sessionUser() {
        return currentUserService.get();
    }

    @ModelAttribute("currentUri")
    public String currentUri(HttpServletRequest httpServletRequest) {
        String currentUri = httpServletRequest.getRequestURI();
        if (httpServletRequest.getQueryString() != null) {
            currentUri += "?" + httpServletRequest.getQueryString();
        }
        return currentUri;
    }
}