package com.example.demo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RequestMapping
@Controller
public class FooterController {

    @GetMapping("/privacy-policy")
    public String getPrivacyPolicy() {
        return "privacy-policy";
    }

}
