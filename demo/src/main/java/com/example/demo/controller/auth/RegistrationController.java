package com.example.demo.controller.auth;

import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.user.AuthorizeService;
import com.example.demo.web.security.SessionAuthenticator;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@AllArgsConstructor
@RequestMapping("/register")
public class RegistrationController {

    private final AuthorizeService authorizeService;
    private final SessionAuthenticator sessionAuthenticator;

    @GetMapping
    public String getRegisterForm(Model model) {
        if (!model.containsAttribute("registerForm")) {
            model.addAttribute("registerForm", RegisterFormDTO.getForm());
        }
        return "register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("registerForm") RegisterFormDTO registerFormDTO,
                           HttpServletRequest httpServletRequest,
                           HttpServletResponse httpServletResponse) {
        authorizeService.validateRegister(registerFormDTO);
        SessionUser sessionUser = authorizeService.register(registerFormDTO);
        sessionAuthenticator.authenticate(sessionUser.username(),
                httpServletRequest, httpServletResponse);
        return "redirect:/";
    }
}