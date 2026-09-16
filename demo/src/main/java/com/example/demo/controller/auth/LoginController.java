package com.example.demo.controller.auth;

import com.example.demo.dto.authorize.LoginFormDTO;
import com.example.demo.dto.badges.BadgeCountsDTO;
import com.example.demo.dto.user.SessionUser;
import com.example.demo.service.BadgeService;
import com.example.demo.service.user.AuthorizeService;
import com.example.demo.web.util.NoCacheHeaders;
import com.example.demo.web.util.ReturnUrlHelper;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/login")
@RequiredArgsConstructor
public class LoginController {

    private final AuthorizeService authorizeService;
    private final BadgeService badgeService;
    private final ReturnUrlHelper returnUrlHelper;

    @GetMapping
    public String loginPage(HttpSession httpSession,
                            Model model,
                            HttpServletResponse httpServletResponse) {
        if (httpSession.getAttribute("sessionUser") != null) {
            NoCacheHeaders.applyAndClearSiteData(httpServletResponse);
            return "redirect:/";
        }
        if (!model.containsAttribute("loginForm")) {
            model.addAttribute("loginForm", LoginFormDTO.builder().build());
        }
        return "login";
    }

    @PostMapping
    public String login(@Valid @ModelAttribute("loginForm") LoginFormDTO loginFormDTO,
                        HttpSession httpSession,
                        HttpServletResponse httpServletResponse) {
        SessionUser sessionUser = authorizeService.login(loginFormDTO);
        httpSession.setAttribute("sessionUser", sessionUser);
        try {
            BadgeCountsDTO badges = badgeService.getBadgeCounts(sessionUser.id());
            httpSession.setAttribute("badges", badges);
        } catch (Exception exception) {
            httpSession.setAttribute("badges", BadgeCountsDTO.builder().build());
        }
        String redirectUrl = returnUrlHelper.getReturnUrlOrDefault(httpSession, "/");
        httpServletResponse.setStatus(HttpServletResponse.SC_SEE_OTHER);
        return "redirect:" + redirectUrl;
    }
}