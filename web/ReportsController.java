package com.example.Docusign.web;

import com.example.Docusign.invite.InviteToken;
import com.example.Docusign.invite.InviteTokenRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class ReportsController {

    private final InviteTokenRepository inviteRepo;

    public ReportsController(InviteTokenRepository inviteRepo) {
        this.inviteRepo = inviteRepo;
    }

    @GetMapping("/reports")
    public String reports(Authentication authentication, Model model) {
        String inviterId = getSubject(authentication);
        List<InviteToken> invites = inviterId != null
                ? inviteRepo.findAllByInviterUserIdOrderByCreatedAtDesc(inviterId)
                : List.of();
        model.addAttribute("invites", invites);
        return "reports";
    }

    private String getSubject(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        return authentication.getName();
    }
}
