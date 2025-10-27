package com.example.Docusign.invite;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.Docusign.account.model.MemberRole;
import com.example.Docusign.account.model.AccountType;
import com.example.Docusign.account.service.AccountService;
import com.example.Docusign.account.dto.AccountCreateRequest;
import com.example.Docusign.account.model.IndividualAccount;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import com.example.Docusign.team.model.TeamMember;
import com.example.Docusign.team.repository.TeamMemberRepository;
import com.example.Docusign.team.repository.TeamRepository;
import com.example.Docusign.team.model.Team;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import java.util.Optional;

@Controller
@RequestMapping("/invite")
public class InviteController {
    private static final Logger log = LoggerFactory.getLogger(InviteController.class);

    private final InviteService inviteService;
    private final AccountService accountService;
    private final IndividualAccountRepository individualAccountRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    public InviteController(InviteService inviteService,
                          AccountService accountService,
                          IndividualAccountRepository individualAccountRepository,
                          TeamMemberRepository teamMemberRepository,
                          TeamRepository teamRepository) {
        this.inviteService = inviteService;
        this.accountService = accountService;
        this.individualAccountRepository = individualAccountRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
    }

    @PostMapping("/create")
    public String createInvite(@RequestParam("fullName") String fullName,
                             @RequestParam("email") String email,
                             @RequestParam("role") String role,
                             @RequestParam(value = "accountId", required = false) Long accountId,
                             HttpServletRequest request,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            String inviterEmail = getClaim(authentication, "email");
            String inviterName = getClaim(authentication, "name");
            String inviterId = getSubject(authentication);

            InviteToken token = inviteService.createInvite(fullName, email, role, Duration.ofHours(24));
            token.setInviterUserId(inviterId);
            token.setInviterName(inviterName);
            token.setInviterEmail(inviterEmail);

            if (accountId == null) {
                accountId = getOrCreatePersonalTeam(inviterEmail, inviterName, inviterId);
            } else if (!teamRepository.existsById(accountId)) {
                redirectAttributes.addFlashAttribute("error", "Invalid team ID provided");
                return "redirect:/dashboard";
            }

            token.setAccountId(accountId);
            token = inviteService.save(token);

            String baseUrl = request.getScheme() + "://" + request.getServerName()
                    + ((request.getServerPort() != 80 && request.getServerPort() != 443)
                    ? ":" + request.getServerPort() : "");

            try {
                inviteService.sendInviteEmail(baseUrl, token);
                redirectAttributes.addFlashAttribute("message", "Invitation sent successfully to " + email);
            } catch (Exception e) {
                log.error("Failed to send invitation email", e);
                redirectAttributes.addFlashAttribute("error",
                        "Invitation created but failed to send email: " + e.getMessage());
            }

            return "redirect:/dashboard";

        } catch (Exception e) {
            log.error("Error creating invite", e);
            redirectAttributes.addFlashAttribute("error", "Failed to create invite: " + e.getMessage());
            return "redirect:/dashboard";
        }
    }

    private Long getOrCreatePersonalTeam(String inviterEmail, String inviterName, String inviterId) {
        String resolvedEmail = inviterEmail;
        if (resolvedEmail == null || resolvedEmail.isBlank()) {
            resolvedEmail = (inviterId != null && !inviterId.isBlank()) ? inviterId + "@local" : null;
        }
        if (resolvedEmail == null || resolvedEmail.isBlank()) {
            resolvedEmail = java.util.UUID.randomUUID().toString() + "@local";
        }

        String resolvedName = (inviterName != null && !inviterName.isBlank()) ? inviterName : resolvedEmail;

        IndividualAccount inviter = individualAccountRepository.findByEmail(resolvedEmail).orElse(null);
        if (inviter == null) {
            IndividualAccount ia = new IndividualAccount();
            ia.setEmail(resolvedEmail);
            ia.setName(resolvedName);
            inviter = individualAccountRepository.save(ia);
        }

        Team team = teamRepository.findFirstByOwner_Id(inviter.getId()).orElse(null);
        if (team == null) {
            team = new Team();
            team.setOwner(inviter);
            team.setName(inviter.getName() != null ? inviter.getName() : inviter.getEmail());
            team.setTeamCode(java.util.UUID.randomUUID().toString());
            team = teamRepository.save(team);
        }

        var existing = teamMemberRepository.findByTeamIdAndUserId(team.getId(), inviter.getId());
        if (existing.isEmpty()) {
            TeamMember member = new TeamMember();
            member.setTeamId(team.getId());
            member.setTeamCode(team.getTeamCode());
            member.setUserId(inviter.getId());
            member.setDisplayName(inviter.getName());
            member.setEmail(inviter.getEmail());
            member.setRole("owner");
            member.setStatus("accepted");
            member.setJoinedAt(java.time.Instant.now());
            teamMemberRepository.save(member);
        }

        return team.getId();
    }

    @GetMapping("/accept")
    public String accept(@RequestParam("token") String token, Model model, Authentication authentication, RedirectAttributes redirectAttributes) {
        Optional<InviteToken> it = inviteService.findByToken(token);
        if (it.isEmpty() || !inviteService.isValid(it.get())) {
            return "invite-invalid";
        }
        // Require login and ensure individual account exists before showing accept form
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to view the invitation.");
            return "redirect:/login";
        }

        // Ensure the user has at least one account (create a personal INDIVIDUAL account if none exist)
        var myAccounts = accountService.listMyAccounts(authentication);
        if (myAccounts == null || myAccounts.isEmpty()) {
            String displayName = getClaim(authentication, "preferred_username");
            if (displayName == null || displayName.isBlank()) {
                displayName = getClaim(authentication, "name");
            }
            if (displayName == null || displayName.isBlank()) {
                displayName = "My Team";
            }
            AccountCreateRequest req = new AccountCreateRequest();
            req.setName(displayName + " Team");
            req.setType(AccountType.INDIVIDUAL);
            try {
                accountService.createAccount(req, null, authentication);
            } catch (Exception ignored) { /* if name exists, ignore */ }
        }
        // Provide team name for display if token has target account/team id
        try {
            Long tid = it.get().getAccountId();
            if (tid != null) {
                teamRepository.findById(tid).ifPresent(t -> model.addAttribute("teamName", t.getName()));
            }
        } catch (Exception ignored) {}
        model.addAttribute("invite", it.get());
        return "invite-accept";
    }

    @PostMapping("/accept")
    public String acceptPost(@RequestParam("token") String token,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        Optional<InviteToken> it = inviteService.findByToken(token);
        if (it.isEmpty() || !inviteService.isValid(it.get())) {
            return "invite-invalid";
        }

        // Require login to attach membership securely
        if (authentication == null) {
            redirectAttributes.addFlashAttribute("message", "Please sign in to accept the invite.");
            return "redirect:/login";
        }

        InviteToken inv = it.get();
        // Interpret InviteToken.accountId as teamId in the new schema
        Long teamId = inv.getAccountId();
        if (teamId != null) {
            // Validate target team exists; if not, bail gracefully to avoid 500
            var teamOpt = teamRepository.findById(teamId);
            if (teamOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "The target workspace no longer exists. Ask the inviter to resend the invite.");
                return "redirect:/dashboard";
            }
            Team targetTeam = teamOpt.get();
            // Resolve accepting user individual account
            String inviteEmail = inv.getEmail();
            String loginEmail = getClaim(authentication, "email");
            String loginName = getClaim(authentication, "preferred_username");
            if (loginName == null || loginName.isBlank()) {
                loginName = getClaim(authentication, "name");
            }

            String resolvedEmail = (inviteEmail != null && !inviteEmail.isBlank()) ? inviteEmail : loginEmail;
            String resolvedName = (inv.getFullName() != null && !inv.getFullName().isBlank())
                    ? inv.getFullName()
                    : (loginName != null && !loginName.isBlank() ? loginName : resolvedEmail);

            IndividualAccount user = null;
            if (resolvedEmail != null && !resolvedEmail.isBlank()) {
                user = individualAccountRepository.findByEmail(resolvedEmail).orElse(null);
            }

            if (user == null && loginEmail != null && !loginEmail.isBlank()
                    && (resolvedEmail == null || !loginEmail.equalsIgnoreCase(resolvedEmail))) {
                user = individualAccountRepository.findByEmail(loginEmail).orElse(null);
                if (user != null && (resolvedEmail == null || resolvedEmail.isBlank())) {
                    resolvedEmail = loginEmail;
                }
            }

            if (user == null) {
                if (resolvedEmail == null || resolvedEmail.isBlank()) {
                    redirectAttributes.addFlashAttribute("message", "Unable to resolve your account email for this invite.");
                    return "redirect:/dashboard";
                }
                IndividualAccount ia = new IndividualAccount();
                ia.setEmail(resolvedEmail);
                ia.setName(resolvedName != null && !resolvedName.isBlank() ? resolvedName : resolvedEmail);
                user = individualAccountRepository.save(ia);
            } else {
                boolean needsUpdate = false;
                if (resolvedName != null && !resolvedName.isBlank() && (user.getName() == null || user.getName().isBlank())) {
                    user.setName(resolvedName);
                    needsUpdate = true;
                }
                if (resolvedEmail != null && !resolvedEmail.isBlank() && !resolvedEmail.equalsIgnoreCase(user.getEmail())) {
                    user.setEmail(resolvedEmail);
                    needsUpdate = true;
                }
                if (needsUpdate) {
                    user = individualAccountRepository.save(user);
                }
            }

            boolean userUpdated = false;
            if ((user.getName() == null || user.getName().isBlank()) && resolvedName != null && !resolvedName.isBlank()) {
                user.setName(resolvedName);
                userUpdated = true;
            }
            if ((user.getEmail() == null || user.getEmail().isBlank()) && resolvedEmail != null && !resolvedEmail.isBlank()) {
                user.setEmail(resolvedEmail);
                userUpdated = true;
            }
            if (userUpdated) {
                user = individualAccountRepository.save(user);
            }

            var existing = teamMemberRepository.findByTeamIdAndUserId(teamId, user.getId());
            TeamMember member = existing.orElseGet(TeamMember::new);
            member.setTeamId(teamId);
            member.setTeamCode(targetTeam.getTeamCode());
            member.setUserId(user.getId());
            String resolvedDisplayName = (inv.getFullName() != null && !inv.getFullName().isBlank())
                    ? inv.getFullName()
                    : user.getName();
            if (resolvedDisplayName == null || resolvedDisplayName.isBlank()) {
                resolvedDisplayName = resolvedEmail;
            }
            member.setDisplayName(resolvedDisplayName);

            member.setEmail(resolvedEmail);
            member.setRole(mapTeamRole(inv.getRole()));
            if (!existing.isPresent() || (member.getStatus() == null || !"accepted".equalsIgnoreCase(member.getStatus()))) {
                member.setStatus("accepted");
                member.setJoinedAt(java.time.Instant.now());
            }
            try {
                String inviterEmail = inv.getInviterEmail();
                if (inviterEmail != null) {
                    individualAccountRepository.findByEmail(inviterEmail)
                            .ifPresent(owner -> member.setInvitedBy(owner.getId()));
                }
            } catch (Exception ignored) {}
            teamMemberRepository.save(member);
        }

        // Mark invite accepted and notify inviter
        inv.setAcceptedAt(java.time.Instant.now());
        inv.setAcceptedByUserId(getSubject(authentication));
        inviteService.markUsed(inv);
        inviteService.sendAcceptanceEmailToInviter(inv);
        redirectAttributes.addFlashAttribute("message", "Invite accepted for " + inv.getEmail() + ".");
        return "redirect:/dashboard";
    }

    private MemberRole mapRole(String s) {
        if (s == null) return MemberRole.SIGNER;
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "ADMIN" -> MemberRole.ADMIN;
            case "OWNER" -> MemberRole.OWNER;
            case "VIEWER" -> MemberRole.VIEWER;
            case "SIGNER" -> MemberRole.SIGNER;
            default -> MemberRole.SIGNER;
        };
    }

    private String mapTeamRole(String s) {
        if (s == null) return "member";
        return switch (s.toUpperCase(Locale.ROOT)) {
            case "OWNER" -> "owner";
            case "ADMIN" -> "admin";
            case "VIEWER" -> "viewer";
            case "SIGNER", "MEMBER" -> "member";
            default -> "member";
        };
    }

    private String getSubject(Authentication authentication) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        return authentication.getName();
    }

    private String getClaim(Authentication authentication, String claim) {
        Object principal = authentication.getPrincipal();
        if (principal instanceof Jwt jwt) {
            return jwt.getClaimAsString(claim);
        }
        if (principal instanceof OidcUser oidcUser) {
            try {
                Object val = oidcUser.getClaims().get(claim);
                return val != null ? String.valueOf(val) : null;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
