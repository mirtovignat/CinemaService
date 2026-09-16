package com.example.demo.controller.auth;

import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.user.AuthorizeService;
import com.example.demo.web.util.ReturnUrlHelper;
import jakarta.servlet.http.HttpSession;
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
    private final ReturnUrlHelper returnUrlHelper;

    @GetMapping
    public String getRegisterForm(HttpSession httpSession, Model model) {
        if (httpSession.getAttribute("sessionUser") != null) {
            return "redirect:/";
        }
        model.addAttribute("registerForm", RegisterFormDTO.getForm());
        return "register";
    }

    @PostMapping
    public String register(@Valid @ModelAttribute("registerForm") RegisterFormDTO registerFormDTO,
                           HttpSession httpSession) {
        authorizeService.validateRegister(registerFormDTO);
        SessionUser sessionUser = authorizeService.register(registerFormDTO);
        httpSession.setAttribute("sessionUser", sessionUser);
        httpSession.setAttribute("badges", BadgeCountsDTO.builder().build());
        String redirectUrl = returnUrlHelper.getReturnUrlOrDefault(httpSession, "/");
        return "redirect:" + redirectUrl;
    }
}