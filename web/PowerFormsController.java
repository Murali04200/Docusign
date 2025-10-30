package com.example.Docusign.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PowerFormsController {

    @GetMapping("/powerforms")
    public String powerformsPage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/";
        }
        return "powerforms";
    }
}
