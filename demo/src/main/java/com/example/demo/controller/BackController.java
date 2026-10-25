package com.example.demo.controller;

import com.example.demo.web.util.NavigationUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class BackController {

    @GetMapping("/back")
    public String back(HttpSession httpSession,
                       @RequestParam(value = "fallback", required = false, defaultValue = "/") String fallbackPath) {
        return "redirect:" + NavigationUtils.resolvePreviousPage(httpSession, fallbackPath);
    }
}