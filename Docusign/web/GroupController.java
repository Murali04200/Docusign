package com.example.Docusign.web;

import com.example.Docusign.account.dto.AccountResponse;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import com.example.Docusign.account.model.MemberRole;
import com.example.Docusign.team.model.Team;
import com.example.Docusign.team.model.TeamMember;
import com.example.Docusign.team.repository.TeamMemberRepository;
import com.example.Docusign.team.repository.TeamRepository;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.stream.Collectors;

@Controller
public class GroupController {

    private final IndividualAccountRepository individualAccountRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    public GroupController(IndividualAccountRepository individualAccountRepository,
                           TeamMemberRepository teamMemberRepository,
                           TeamRepository teamRepository) {
        this.individualAccountRepository = individualAccountRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
    }

    @GetMapping("/groups")
    public String groups(Model model, Authentication authentication) {
        // Lookup teams by current user membership
        String email = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
            email = oidc.getEmail();
        }
        if (email == null) {
            model.addAttribute("teams", java.util.List.of());
            return "groups";
        }
        java.util.Optional<com.example.Docusign.account.model.IndividualAccount> optUser;
        try {
            optUser = individualAccountRepository.findByEmail(email);
        } catch (Exception ex) {
            model.addAttribute("teams", java.util.List.of());
            return "groups";
        }
        if (optUser.isEmpty()) {
            model.addAttribute("teams", java.util.List.of());
            return "groups";
        }
        Long userId = optUser.get().getId();
        List<TeamMember> memberships = teamMemberRepository.findByUserIdAndStatus(userId, "accepted");
        if (memberships == null) {
            memberships = java.util.List.of();
        }

        List<Team> ownedTeams = teamRepository.findAllByOwner_Id(userId);
        if (ownedTeams == null) {
            ownedTeams = java.util.List.of();
        }

        Set<Long> teamIds = new LinkedHashSet<>();
        teamIds.addAll(memberships.stream()
                .map(TeamMember::getTeamId)
                .filter(java.util.Objects::nonNull)
                .toList());
        teamIds.addAll(ownedTeams.stream()
                .map(Team::getId)
                .filter(java.util.Objects::nonNull)
                .toList());

        if (teamIds.isEmpty()) {
            model.addAttribute("teams", java.util.List.of());
            return "groups";
        }

        List<Team> teams = teamRepository.findAllById(teamIds);
        Map<Long, String> roleByTeam = memberships.stream()
                .filter(tm -> tm.getTeamId() != null)
                .collect(Collectors.toMap(TeamMember::getTeamId, TeamMember::getRole, (a, b) -> a));
        for (Team owned : ownedTeams) {
            if (owned != null && owned.getId() != null) {
                roleByTeam.putIfAbsent(owned.getId(), "owner");
            }
        }

        // Map to simple DTO-like structure using existing AccountResponse as a view model
        List<AccountResponse> view = teams.stream().map(t -> {
            try {
                AccountResponse ar = new AccountResponse();
                ar.setId(t.getId());
                ar.setName(t.getName());
                ar.setType(com.example.Docusign.account.model.AccountType.GROUP);
                // populate team members
                List<TeamMember> teamMs = teamMemberRepository.findByTeamId(t.getId());
                if (teamMs == null) teamMs = java.util.List.of();
                List<AccountResponse.MemberSummary> ms = teamMs.stream()
                        .filter(tm -> tm.getStatus() == null || "accepted".equalsIgnoreCase(tm.getStatus()))
                        .map(tm -> {
                            AccountResponse.MemberSummary s = new AccountResponse.MemberSummary();
                            String disp = tm.getDisplayName();
                            if (disp == null || disp.isBlank()) {
                                disp = resolveDisplayName(tm.getUserId(), tm.getEmail());
                            }
                            s.setDisplayName(disp != null ? disp : "Member");

                            String emailSnapshot = tm.getEmail();
                            if (emailSnapshot == null || emailSnapshot.isBlank()) {
                                emailSnapshot = resolveEmail(tm.getUserId());
                            }
                            s.setEmail(emailSnapshot);

                            s.setRole(mapToMemberRole(tm.getRole()));
                            s.setJoinedAt(tm.getJoinedAt());
                            s.setKeycloakUserId(null);
                            return s;
                        })
                        .sorted(java.util.Comparator.comparing(AccountResponse.MemberSummary::getDisplayName, java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList();
                // owner display name
                try {
                    var ownerMember = teamMs.stream()
                            .filter(m -> "owner".equalsIgnoreCase(m.getRole()))
                            .findFirst();
                    if (ownerMember.isPresent()) {
                        var owner = individualAccountRepository.findById(ownerMember.get().getUserId());
                        ar.setOwnerDisplayName(owner.map(u -> u.getName() != null ? u.getName() : u.getEmail()).orElse("Owner"));
                    }
                } catch (Exception ignored) {}
                ar.setMembers(ms);
                return ar;
            } catch (Exception ex) {
                // In case of any unexpected error, return a minimal card to avoid 500s
                AccountResponse ar = new AccountResponse();
                ar.setId(t != null ? t.getId() : null);
                ar.setName(t != null ? t.getName() : "Team");
                ar.setType(com.example.Docusign.account.model.AccountType.GROUP);
                ar.setMembers(java.util.List.of());
                return ar;
            }
        }).toList();
        // attach role map for template consumption if needed
        model.addAttribute("teamRoles", roleByTeam);
        model.addAttribute("teams", view);
        return "groups";
    }

    private MemberRole mapToMemberRole(String r) {
        if (r == null) return MemberRole.SIGNER;
        switch (r.toLowerCase()) {
            case "owner": return MemberRole.OWNER;
            case "admin": return MemberRole.ADMIN;
            case "viewer": return MemberRole.VIEWER;
            case "member": return MemberRole.SIGNER;
            default: return MemberRole.SIGNER;
        }
    }

    private String resolveDisplayName(Long userId, String fallbackEmail) {
        if (userId != null) {
            try {
                return individualAccountRepository.findById(userId)
                        .map(u -> u.getName() != null && !u.getName().isBlank() ? u.getName() : u.getEmail())
                        .orElse(fallbackEmail);
            } catch (Exception ignored) {
                return fallbackEmail;
            }
        }
        return fallbackEmail;
    }

    private String resolveEmail(Long userId) {
        if (userId == null) {
            return null;
        }
        try {
            return individualAccountRepository.findById(userId)
                    .map(com.example.Docusign.account.model.IndividualAccount::getEmail)
                    .orElse(null);
        } catch (Exception ignored) {
            return null;
        }
    }
}

