package com.example.demo.controller.auth;

import com.example.demo.dto.user.SessionUser;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class AuthStatusController {

    @GetMapping("/auth/status")
    @ResponseBody
    public ResponseEntity<Void> status(
            SessionUser sessionUser) {
        if (sessionUser == null) {
            return ResponseEntity.status(401).build();
        }
        return ResponseEntity.ok().build();
    }
}