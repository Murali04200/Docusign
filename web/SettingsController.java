package com.example.Docusign.web;

import com.example.Docusign.account.model.IndividualAccount;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class SettingsController {

    private static final Logger log = LoggerFactory.getLogger(SettingsController.class);

    private final IndividualAccountRepository accountRepository;

    public SettingsController(IndividualAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    @GetMapping("/settings")
    public String settingsPage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/";
        }

        try {
            String email = getEmailFromAuthentication(authentication);

            if (email != null) {
                Optional<IndividualAccount> accountOpt = accountRepository.findByEmail(email);
                if (accountOpt.isPresent()) {
                    IndividualAccount account = accountOpt.get();
                    model.addAttribute("userName", account.getName());
                    model.addAttribute("userEmail", account.getEmail());
                } else {
                    model.addAttribute("userName", "");
                    model.addAttribute("userEmail", email);
                }
            }

        } catch (Exception e) {
            log.error("Error loading user information for settings page", e);
            model.addAttribute("userName", "");
        }

        return "settings";
    }

    private String getEmailFromAuthentication(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            String email = oidcUser.getEmail();
            if (email == null) {
                email = oidcUser.getPreferredUsername();
            }
            return email;
        } else if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String email = jwt.getClaimAsString("email");
            if (email == null) {
                email = jwt.getClaimAsString("preferred_username");
            }
            return email;
        }
        return null;
    }
}
