package com.example.demo.controller.auth;

import com.example.demo.dto.authorize.LoginFormDTO;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/login")
public class LoginController {

    @GetMapping
    public String loginPage(Model model,
                            @RequestParam(value = "switch", required = false) String switchUsername) {
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", buildLoginForm(switchUsername));
        }
        return "auth/login";
    }

    private LoginFormDTO buildLoginForm(String switchUsername) {
        if (switchUsername != null && !switchUsername.isBlank()) {
            return LoginFormDTO.forSwitch(switchUsername);
        }
        return LoginFormDTO.builder()
                .build();
    }
}