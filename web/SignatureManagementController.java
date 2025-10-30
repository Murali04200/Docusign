package com.example.Docusign.web;

import com.example.Docusign.account.model.IndividualAccount;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Optional;

@Controller
public class SignatureManagementController {

    private static final Logger log = LoggerFactory.getLogger(SignatureManagementController.class);

    private final IndividualAccountRepository accountRepository;

    public SignatureManagementController(IndividualAccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    /**
     * Display the signature management page
     */
    @GetMapping("/signatures")
    public String signaturesPage(Authentication authentication, Model model) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return "redirect:/";
        }

        try {
            // Get authenticated user's email from JWT
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String email = jwt.getClaimAsString("email");

            if (email == null) {
                email = jwt.getClaimAsString("preferred_username");
            }

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
            log.error("Error loading user information for signatures page", e);
            model.addAttribute("userName", "");
        }

        return "signatures";
    }
}
