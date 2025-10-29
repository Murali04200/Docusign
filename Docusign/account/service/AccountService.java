package com.example.Docusign.account.service;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Docusign.account.dto.AccountCreateRequest;
import com.example.Docusign.account.dto.AccountResponse;
import com.example.Docusign.account.model.AccountType;
import com.example.Docusign.account.model.IndividualAccount;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import com.example.Docusign.team.model.Team;
import com.example.Docusign.team.model.TeamMember;
import com.example.Docusign.team.repository.TeamMemberRepository;
import com.example.Docusign.team.repository.TeamRepository;

@Service
@Transactional
public class AccountService {

    private final IndividualAccountRepository individualAccountRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;

    public AccountService(IndividualAccountRepository individualAccountRepository,
                          TeamMemberRepository teamMemberRepository,
                          TeamRepository teamRepository) {
        this.individualAccountRepository = individualAccountRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
    }

    public AccountResponse createAccount(AccountCreateRequest request, Principal principal, Authentication authentication) {
        // No-op stub to keep controllers functioning during refactor
        return new AccountResponse();
    }

    public List<AccountResponse> listMyAccounts(Authentication authentication) {
        if (authentication == null) {
            return List.of();
        }

        OidcUser oidcUser = authentication.getPrincipal() instanceof OidcUser ?
                (OidcUser) authentication.getPrincipal() : null;

        String email = oidcUser != null ? oidcUser.getEmail() : null;
        if (email == null || email.isBlank()) {
            return List.of();
        }

        IndividualAccount currentUser = individualAccountRepository.findByEmail(email).orElse(null);
        if (currentUser == null || currentUser.getId() == null) {
            return List.of();
        }

        Long userId = currentUser.getId();

        List<AccountResponse> results = new java.util.ArrayList<>();

        // Personal workspace representation
        // NOTE: Do NOT expose personal email to protect user privacy
        AccountResponse personal = new AccountResponse();
        personal.setId(userId);
        personal.setName(currentUser.getName());
        personal.setType(AccountType.INDIVIDUAL);
        // personal.setOwnerEmail(currentUser.getEmail()); // REMOVED: Privacy protection
        results.add(personal);

        // Team memberships
        List<TeamMember> memberships = teamMemberRepository.findByUserId(userId);
        if (memberships != null) {
            java.util.Set<Long> seen = new java.util.LinkedHashSet<>();
            seen.add(userId);

            for (TeamMember member : memberships) {
                if (member == null) continue;
                if (member.getStatus() != null && "removed".equalsIgnoreCase(member.getStatus())) {
                    continue;
                }

                Long teamId = member.getTeamId();
                if ((teamId == null || teamId == 0) && member.getTeamCode() != null) {
                    teamId = teamRepository.findByTeamCode(member.getTeamCode())
                            .map(Team::getId)
                            .orElse(null);
                }
                if (teamId == null || seen.contains(teamId)) {
                    continue;
                }

                String teamName = teamRepository.findById(teamId)
                        .map(Team::getName)
                        .orElse("Workspace");

                AccountResponse resp = new AccountResponse();
                resp.setId(teamId);
                resp.setName(teamName);
                resp.setType(AccountType.GROUP);
                resp.setOwnerDisplayName(member.getRole());
                results.add(resp);
                seen.add(teamId);
            }
        }

        return results;
    }

    public AccountResponse getAccountDetails(Long accountId, Authentication authentication) {
        throw new IllegalArgumentException("Account API disabled");
    }

    public void removeMember(Long accountId, String memberKeycloakId, Authentication authentication) {
        throw new AccessDeniedException("Account API disabled");
    }

    // note: Individual account user records are now in `individual_accounts` table
    // and managed separately from workspace `accounts`.
}
