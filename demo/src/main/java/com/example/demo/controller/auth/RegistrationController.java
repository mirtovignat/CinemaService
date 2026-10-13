package com.example.demo.controller.auth;

import com.example.demo.dto.authorize.RegisterFormDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.user.AuthorizeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
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
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

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
        SessionUser registeredSessionUser = authorizeService.register(registerFormDTO);

        httpServletRequest.changeSessionId();

        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        registeredSessionUser.username(),
                        registerFormDTO.rawPassword())
        );

        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);
        securityContextRepository.saveContext(securityContext, httpServletRequest, httpServletResponse);

        return "redirect:/";
    }
}