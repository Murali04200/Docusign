package com.example.Docusign.team;

import com.example.Docusign.team.model.TeamInvite;
import com.example.Docusign.team.model.Team;
import com.example.Docusign.team.repository.TeamInviteRepository;
import com.example.Docusign.team.repository.TeamRepository;
import com.example.Docusign.mail.InviteMailService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Controller
@RequestMapping("/team-invite")
public class TeamInviteController {
    private static final Logger log = LoggerFactory.getLogger(TeamInviteController.class);

    private final TeamRepository teamRepository;
    private final TeamInviteRepository inviteRepository;
    private final InviteMailService inviteMailService;

    public TeamInviteController(TeamRepository teamRepository,
                                TeamInviteRepository inviteRepository,
                                InviteMailService inviteMailService) {
        this.teamRepository = teamRepository;
        this.inviteRepository = inviteRepository;
        this.inviteMailService = inviteMailService;
    }

    @GetMapping
    public String showForm(@RequestParam(value = "success", required = false) String success,
                           @RequestParam(value = "message", required = false) String message,
                           Model model) {
        List<Team> teams = teamRepository.findAll();
        model.addAttribute("teams", teams);
        if (success != null) {
            model.addAttribute("success", true);
            model.addAttribute("message", message != null ? message : "Invite sent successfully.");
        }
        return "team/invite";
    }

    @PostMapping
    public String submitInvite(@RequestParam("teamId") Long teamId,
                               @RequestParam("fullName") String fullName,
                               @RequestParam("email") String email,
                               @RequestParam("role") String role,
                               RedirectAttributes ra) {
        TeamInvite invite = new TeamInvite();
        invite.setTeamId(teamId);
        invite.setInvitedUserEmail(email);
        invite.setToken(UUID.randomUUID().toString());
        invite.setStatus("pending");
        // expire in 24 hours
        invite.setExpiresAt(Instant.now().plus(24, ChronoUnit.HOURS));
        // senderName/senderEmail available but TeamInvite does not store; could be logged separately.
        // We don't have explicit fields for invitee name/role on TeamInvite; store minimal data
        inviteRepository.save(invite);

        // Try to fetch team for nicer email content
        String teamName = null;
        try {
            teamName = teamRepository.findById(teamId).map(Team::getName).orElse(null);
        } catch (Exception ignored) {}

        // Build absolute accept link (e.g., http://host:port/team-invite/accept?token=...)
        String inviteLink = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/team-invite/accept")
                .queryParam("token", invite.getToken())
                .build()
                .toUriString();
        try {
            // Prefer HTML email for clickable link; fallback to text if needed
            inviteMailService.sendTeamInviteHtml(email, fullName, role, teamName, inviteLink);
            ra.addAttribute("success", "1");
            ra.addAttribute("message", "Invite sent to " + email + ".");
        } catch (Exception mailEx) {
            log.warn("HTML mail failed, trying text-only: {}", mailEx.getMessage());
            try {
                inviteMailService.sendTeamInvite(email, fullName, role, teamName, inviteLink);
                ra.addAttribute("success", "1");
                ra.addAttribute("message", "Invite sent to " + email + ".");
            } catch (Exception mailEx2) {
                log.error("Failed to send invite email to {}: {}", email, mailEx2.getMessage(), mailEx2);
                ra.addAttribute("success", "0");
                ra.addAttribute("message", "Invite saved, but email failed to send. Please check mail settings.");
            }
        }
        return "redirect:/team-invite";
    }

    @GetMapping("/accept")
    public String acceptInvitePage(@RequestParam("token") String token, Model model, RedirectAttributes ra) {
        var opt = inviteRepository.findByToken(token);
        if (opt.isEmpty()) {
            return "invite-invalid";
        }
        var invite = opt.get();
        if ("accepted".equalsIgnoreCase(invite.getStatus())) {
            return "invite-invalid";
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            return "invite-invalid";
        }
        String teamName = teamRepository.findById(invite.getTeamId()).map(Team::getName).orElse("Team");
        model.addAttribute("invite", invite);
        model.addAttribute("teamName", teamName);
        return "invite-accept";
    }

    @PostMapping("/accept")
    public String acceptInvite(@RequestParam("token") String token, RedirectAttributes ra) {
        var opt = inviteRepository.findByToken(token);
        if (opt.isEmpty()) {
            ra.addAttribute("success", "0");
            ra.addAttribute("message", "Invalid invite token.");
            return "redirect:/team-invite";
        }
        var invite = opt.get();
        if ("accepted".equalsIgnoreCase(invite.getStatus())) {
            ra.addAttribute("success", "0");
            ra.addAttribute("message", "This invite has already been used.");
            return "redirect:/team-invite";
        }
        if (invite.getExpiresAt() != null && invite.getExpiresAt().isBefore(Instant.now())) {
            ra.addAttribute("success", "0");
            ra.addAttribute("message", "This invite link has expired.");
            return "redirect:/team-invite";
        }
        // mark accepted
        invite.setAcceptedAt(java.time.Instant.now());
        invite.setStatus("accepted");
        inviteRepository.save(invite);

        // Notify sender (team owner) if available
        try {
            var teamOpt = teamRepository.findById(invite.getTeamId());
            if (teamOpt.isPresent() && teamOpt.get().getOwner() != null && teamOpt.get().getOwner().getEmail() != null) {
                String ownerEmail = teamOpt.get().getOwner().getEmail();
                String teamName = teamOpt.get().getName();
                inviteMailService.sendInviteAccepted(ownerEmail, invite.getInvitedUserEmail(), teamName);
            }
        } catch (Exception notifyEx) {
            log.warn("Failed to notify team owner of accepted invite: {}", notifyEx.getMessage());
        }

        ra.addAttribute("success", "1");
        ra.addAttribute("message", "Invite accepted. You can now access the team once provisioned.");
        return "redirect:/team-invite";
    }
}
