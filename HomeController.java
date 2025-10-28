package com.example.Docusign.web;

import com.example.Docusign.account.dto.AccountResponse;
import com.example.Docusign.account.model.AccountType;
import com.example.Docusign.account.service.AccountService;
import com.example.Docusign.model.DocumentRepository;
import com.example.Docusign.workspace.ActiveAccountInterceptor;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import com.example.Docusign.account.model.IndividualAccount;
import com.example.Docusign.team.repository.TeamMemberRepository;
import com.example.Docusign.team.repository.TeamRepository;
import com.example.Docusign.team.model.TeamMember;
import com.example.Docusign.team.model.Team;
import com.example.Docusign.team.activity.TeamActivityService;
import com.example.Docusign.invite.InviteToken;
import com.example.Docusign.invite.InviteTokenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestAttributes;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Locale;

@Controller
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final AccountService accountService;
    private final DocumentRepository documentRepository;
    private final IndividualAccountRepository individualAccountRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final TeamRepository teamRepository;
    private final InviteTokenRepository inviteTokenRepository;
    private final TeamActivityService teamActivityService;
    public HomeController(AccountService accountService, DocumentRepository documentRepository,
                          IndividualAccountRepository individualAccountRepository,
                          TeamMemberRepository teamMemberRepository,
                          TeamRepository teamRepository,
                          InviteTokenRepository inviteTokenRepository,
                          TeamActivityService teamActivityService) {
        this.accountService = accountService;
        this.documentRepository = documentRepository;
        this.individualAccountRepository = individualAccountRepository;
        this.teamMemberRepository = teamMemberRepository;
        this.teamRepository = teamRepository;
        this.inviteTokenRepository = inviteTokenRepository;
        this.teamActivityService = teamActivityService;
    }

    @GetMapping("/")
    public String home() {
        return "login";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OidcUser oidcUser, Model model, Authentication authentication) {
        logger.info("Accessing dashboard endpoint");

        boolean teamViewActive = false;

        try {
            // Set user info in model
            if (oidcUser != null) {
                model.addAttribute("username", oidcUser.getAttribute("preferred_username"));
                model.addAttribute("email", oidcUser.getEmail() != null ? oidcUser.getEmail() : oidcUser.getName() + "@example.com");
                model.addAttribute("name", oidcUser.getGivenName() != null ? oidcUser.getGivenName() : oidcUser.getName());
                model.addAttribute("userRoles", SecurityContextHolder.getContext().getAuthentication().getAuthorities());
                // Upsert IndividualAccount from Keycloak login
                try {
                    String emailStr = oidcUser.getEmail();
                    String displayName = oidcUser.getGivenName() != null ? oidcUser.getGivenName() : oidcUser.getName();
                    if (emailStr != null && !emailStr.isBlank()) {
                        var accOpt = individualAccountRepository.findByEmail(emailStr);
                        IndividualAccount ia;
                        if (accOpt.isEmpty()) {
                            ia = new IndividualAccount();
                            ia.setEmail(emailStr);
                            ia.setName(displayName != null ? displayName : emailStr);
                            ia = individualAccountRepository.save(ia);
                        } else {
                            ia = accOpt.get();
                            boolean changed = false;
                            if (displayName != null && (ia.getName() == null || !ia.getName().equals(displayName))) {
                                ia.setName(displayName);
                                changed = true;
                            }
                            if (ia.getEmailAccountId() == null || ia.getEmailAccountId().isBlank()) {
                                String code = java.util.UUID.randomUUID().toString().replaceAll("-", "").toUpperCase().substring(0, 10);
                                ia.setEmailAccountId(code);
                                changed = true;
                            }
                            if (changed) ia = individualAccountRepository.save(ia);
                        }
                        // Ensure personal team exists (team.id == ia.id)
                        try {
                            var existingTeam = teamRepository.findFirstByOwner_Id(ia.getId());
                            if (existingTeam.isEmpty()) {
                                Team t = new Team();
                                t.setOwner(ia); // @MapsId will set team.id = ia.id
                                t.setName(ia.getName() != null ? ia.getName() : emailStr);
                                t.setTeamCode(java.util.UUID.randomUUID().toString());
                                teamRepository.save(t);
                            }
                        } catch (Exception ignored) { }
                    }
                } catch (Exception ignored) { }
                // Profile Account Code (first 8 chars)
                try {
                    String emailStr = oidcUser.getEmail();
                    if (emailStr != null) {
                        individualAccountRepository.findByEmail(emailStr).ifPresent(acc -> {
                            String code = acc.getEmailAccountId();
                            if (code != null && code.length() > 8) code = code.substring(0,8);
                            model.addAttribute("profileAccountCode", code);
                        });
                    }
                } catch (Exception ignored) {
                    model.addAttribute("profileAccountCode", null);
                }
            } else {
                model.addAttribute("username", "guest");
                model.addAttribute("email", "guest@example.com");
                model.addAttribute("name", "Guest");
                model.addAttribute("userRoles", List.of());
                model.addAttribute("profileAccountCode", null);
            }

            // Placeholder; actual values will be loaded after determining active account
            model.addAttribute("pendingCount", 0);
            model.addAttribute("completedCount", 0);
            model.addAttribute("draftCount", 0);
            model.addAttribute("recentDocuments", List.of());
            model.addAttribute("startSignatureUrl", "/sign");

            // Account handling
            List<NotificationItem> notifications = java.util.List.of();

            if (authentication != null) {
                // Start with accounts from service (may be empty or missing recent memberships)
                List<AccountResponse> accounts = accountService.listMyAccounts(authentication);
                List<AccountResponse> membershipAccounts = new java.util.ArrayList<>();
                List<TeamMember> memberships = new java.util.ArrayList<>();

                // Build/merge accounts from accepted team memberships so newly accepted teams appear immediately
                try {
                    Long uid = null;
                    String email = oidcUser != null ? oidcUser.getEmail() : null;
                    java.util.Optional<com.example.Docusign.account.model.IndividualAccount> userOpt =
                            (email != null) ? individualAccountRepository.findByEmail(email) : java.util.Optional.empty();
                    com.example.Docusign.account.model.IndividualAccount user = null;
                    if (userOpt.isPresent()) {
                        user = userOpt.get();
                        uid = user.getId();
                    }

                    memberships = (uid != null)
                            ? teamMemberRepository.findByUserId(uid).stream()
                                    .filter(tm -> tm != null && (tm.getStatus() == null || "accepted".equalsIgnoreCase(tm.getStatus())))
                                    .toList()
                            : java.util.List.of();

                    // If absolutely nothing exists, auto-create a personal team (team.id == user.id) and owner membership
                    if ((accounts == null || accounts.isEmpty()) && uid != null && (memberships == null || memberships.isEmpty())) {
                        Team t = new Team();
                        t.setName((user != null && user.getName() != null) ? user.getName() : (email != null ? email : "My"));
                        t.setOwner(user); // @MapsId ensures team.id == user.id
                        t.setTeamCode(java.util.UUID.randomUUID().toString());
                        t = teamRepository.save(t);
                        TeamMember m = new TeamMember();
                        m.setTeamId(t.getId());
                        m.setTeamCode(t.getTeamCode());
                        m.setUserId(user != null ? user.getId() : null);
                        m.setDisplayName(user != null ? user.getName() : null);
                        m.setEmail(user != null ? user.getEmail() : null);
                        m.setRole("owner");
                        m.setStatus("accepted");
                        m.setJoinedAt(java.time.Instant.now());
                        teamMemberRepository.save(m);
                        try {
                            RequestContextHolder.currentRequestAttributes()
                                .setAttribute(ActiveAccountInterceptor.SESSION_KEY, String.valueOf(t.getId()), RequestAttributes.SCOPE_SESSION);
                        } catch (Exception ignored) {}
                        accounts = java.util.List.of();
                        memberships = java.util.List.of(m);
                    }

                    // Construct team accounts from memberships, resolving team details via team repository or association
                    if (memberships != null && !memberships.isEmpty()) {
                        java.util.Map<Long, AccountResponse> membershipAccountMap = new java.util.LinkedHashMap<>();
                        java.util.Map<Long, String> membershipCodes = new java.util.HashMap<>();

                        for (TeamMember tm : memberships) {
                            if (tm == null) continue;
                            Long teamId = tm.getTeamId();
                            String teamCode = tm.getTeamCode();

                            if ((teamId == null || teamId == 0) && teamCode != null && !teamCode.isBlank()) {
                                var teamOpt = teamRepository.findByTeamCode(teamCode);
                                if (teamOpt.isPresent()) {
                                    Team team = teamOpt.get();
                                    teamId = team.getId();
                                }
                            }

                            if (teamId == null) continue;

                            final Long resolvedTeamId = teamId;
                            AccountResponse ar = membershipAccountMap.computeIfAbsent(resolvedTeamId, key -> {
                                AccountResponse resp = new AccountResponse();
                                resp.setId(resolvedTeamId);
                                resp.setType(AccountType.GROUP);
                                return resp;
                            });

                            if (ar.getName() == null && teamCode != null) {
                                membershipCodes.put(resolvedTeamId, teamCode);
                            }
                        }

                        if (!membershipAccountMap.isEmpty()) {
                            var teams = teamRepository.findAllById(membershipAccountMap.keySet());
                            for (Team team : teams) {
                                if (team != null && team.getId() != null && membershipAccountMap.containsKey(team.getId())) {
                                    membershipAccountMap.get(team.getId()).setName(team.getName());
                                }
                            }

                            for (var entry : membershipAccountMap.entrySet()) {
                                AccountResponse ar = entry.getValue();
                                if ((ar.getName() == null || ar.getName().isBlank()) && membershipCodes.containsKey(entry.getKey())) {
                                    String code = membershipCodes.get(entry.getKey());
                                    teamRepository.findByTeamCode(code)
                                            .ifPresent(t -> ar.setName(t.getName()));
                                    if (ar.getName() == null || ar.getName().isBlank()) {
                                        ar.setName("Team " + code);
                                    }
                                }
                            }
                        }

                        membershipAccounts = new java.util.ArrayList<>(membershipAccountMap.values());
                    } else {
                        membershipAccounts = new java.util.ArrayList<>();
                    }

                    // Merge service accounts and membership accounts by id
                    java.util.Map<Long, AccountResponse> merged = new java.util.LinkedHashMap<>();
                    if (accounts != null) {
                        for (AccountResponse ar : accounts) {
                            if (ar != null && ar.getId() != null) merged.put(ar.getId(), ar);
                        }
                    }
                    for (AccountResponse ar : membershipAccounts) {
                        if (ar != null && ar.getId() != null) merged.putIfAbsent(ar.getId(), ar);
                    }
                    accounts = new java.util.ArrayList<>(merged.values());
                    // Sort by name case-insensitive
                    accounts.sort(java.util.Comparator.comparing(AccountResponse::getName, String.CASE_INSENSITIVE_ORDER));

                } catch (Exception ex) {
                    // On any error, fall back to service accounts list
                    if (accounts == null) accounts = List.of();
                    membershipAccounts = new java.util.ArrayList<>();
                    memberships = new java.util.ArrayList<>();
                }

                model.addAttribute("myAccounts", accounts);

                // Build notifications for invites related to current user
                try {
                    String inviterSubject = getSubject(authentication);
                    if (inviterSubject != null) {
                        var tokens = inviteTokenRepository.findAllByInviterUserIdOrderByCreatedAtDesc(inviterSubject);
                        if (tokens != null && !tokens.isEmpty()) {
                            var accountIds = tokens.stream()
                                    .map(InviteToken::getAccountId)
                                    .filter(java.util.Objects::nonNull)
                                    .distinct()
                                    .toList();
                            java.util.Map<Long, String> teamNames = accountIds.isEmpty()
                                    ? java.util.Map.of()
                                    : teamRepository.findAllById(accountIds).stream()
                                            .collect(java.util.stream.Collectors.toMap(Team::getId, Team::getName));

                            notifications = tokens.stream()
                                    .map(token -> NotificationItem.from(token, teamNames.get(token.getAccountId())))
                                    .filter(java.util.Objects::nonNull)
                                    .limit(15)
                                    .toList();
                        }
                    }
                } catch (Exception ignored) {
                    notifications = java.util.List.of();
                }

                // Build Personal account from current IndividualAccount
                com.example.Docusign.account.model.IndividualAccount currentUser = null;
                try {
                    String email = oidcUser != null ? oidcUser.getEmail() : null;
                    if (email != null) {
                        var userOpt = individualAccountRepository.findByEmail(email);
                        if (userOpt.isPresent()) currentUser = userOpt.get();
                    }
                } catch (Exception ignored) {}

                AccountResponse personal = null;
                String personalTeamName = null;
                if (currentUser != null) {
                    // Derive personal team name from Team table, fallback to user's name
                    try {
                        var pTeamOpt = teamRepository.findFirstByOwner_Id(currentUser.getId());
                        if (pTeamOpt.isPresent()) {
                            personalTeamName = pTeamOpt.get().getName();
                        } else {
                            personalTeamName = currentUser.getName();
                        }
                    } catch (Exception ignored) {
                        personalTeamName = currentUser.getName();
                    }
                    personal = new AccountResponse();
                    personal.setId(currentUser.getId());
                    personal.setName(personalTeamName);
                    personal.setType(AccountType.INDIVIDUAL);
                }

                java.util.Map<Long, String> membershipRoles = new java.util.HashMap<>();
                if (memberships == null) {
                    memberships = java.util.List.of();
                }
                java.util.Set<Long> ownedTeamIds = new java.util.LinkedHashSet<>();
                for (TeamMember tm : memberships) {
                    if (tm == null) continue;
                    Long tid = tm.getTeamId();
                    if ((tid == null || tid == 0) && tm.getTeamCode() != null && !tm.getTeamCode().isBlank()) {
                        tid = teamRepository.findByTeamCode(tm.getTeamCode()).map(Team::getId).orElse(null);
                    }
                    if (tid != null && tm.getRole() != null) {
                        membershipRoles.put(tid, tm.getRole());
                    }
                }

                List<AccountResponse> teams = membershipAccounts.stream()
                        .filter(ar -> ar != null && ar.getId() != null)
                        .map(ar -> {
                            AccountResponse copy = new AccountResponse();
                            copy.setId(ar.getId());
                            copy.setName(ar.getName());
                            copy.setType(ar.getType());
                            if (membershipRoles.containsKey(ar.getId())) {
                                copy.setOwnerDisplayName(membershipRoles.get(ar.getId()));
                            } else {
                                copy.setOwnerDisplayName(ar.getOwnerDisplayName());
                            }
                            return copy;
                        })
                        .sorted(java.util.Comparator.comparing(AccountResponse::getName,
                                java.util.Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                        .toList();

                model.addAttribute("personalAccount", personal);
                model.addAttribute("personalTeamName", personalTeamName);
                if (personal != null && personal.getId() != null) {
                    ownedTeamIds.add(personal.getId());
                    try {
                        teamRepository.findAllByOwner_Id(personal.getId()).stream()
                                .map(Team::getId)
                                .filter(java.util.Objects::nonNull)
                                .forEach(ownedTeamIds::add);
                    } catch (Exception ignored) { }
                }

                Map<Long, List<TeamMember>> teamMembersByTeam = new LinkedHashMap<>();
                for (AccountResponse team : teams) {
                    if (team == null || team.getId() == null) continue;
                    if (!ownedTeamIds.isEmpty() && !ownedTeamIds.contains(team.getId())) {
                        continue; // only fetch members for teams owned by the current user
                    }
                    try {
                        List<TeamMember> memberList = teamMemberRepository.findByTeamId(team.getId());
                        List<TeamMember> filtered = (memberList == null)
                                ? List.of()
                                : memberList.stream()
                                        .filter(tm -> tm != null && (tm.getStatus() == null || !"removed".equalsIgnoreCase(tm.getStatus())))
                                        .toList();
                        teamMembersByTeam.put(team.getId(), filtered);
                    } catch (Exception ignored) {
                        teamMembersByTeam.put(team.getId(), List.of());
                    }
                }

                model.addAttribute("teamAccounts", teams);
                model.addAttribute("teamMemberDetails", teamMembersByTeam);
                model.addAttribute("userHasTeams", !teams.isEmpty());
                model.addAttribute("ownedTeamIds", ownedTeamIds);

                // Set activeAccountId from session
                Long activeAccountId = getActiveAccountIdFromSession();
                if (activeAccountId == null && personal != null) {
                    activeAccountId = personal.getId();
                }
                model.addAttribute("activeAccountId", activeAccountId);
                model.addAttribute("individualAccountCode", activeAccountId);
                // Also provide the team's code from Team table for the active workspace
                try {
                    if (activeAccountId != null) {
                        teamRepository.findById(activeAccountId)
                                .ifPresent(t -> model.addAttribute("activeTeamCode", t.getTeamCode()));
                    } else {
                        model.addAttribute("activeTeamCode", null);
                    }
                } catch (Exception ignored) {
                    model.addAttribute("activeTeamCode", null);
                }
                boolean personalSelected = personal != null
                        && activeAccountId != null
                        && activeAccountId.equals(personal.getId());
                // Flag: true when the active workspace is not the personal workspace
                boolean isTeamActive = activeAccountId != null && !personalSelected;
                model.addAttribute("isTeamActive", isTeamActive);
                teamViewActive = isTeamActive;

                // Compute active account name from Team table (so we don't show literal 'Personal Account')
                String activeAccountName = null;
                try {
                    if (activeAccountId != null) {
                        var teamOpt = teamRepository.findById(activeAccountId);
                        if (teamOpt.isPresent()) {
                            activeAccountName = teamOpt.get().getName();
                        } else if (personalSelected && currentUser != null) {
                            activeAccountName = currentUser.getName();
                        }
                    }
                } catch (Exception ignored) {}
                model.addAttribute("activeAccountName", activeAccountName);

                String activeRole = null;

                // Determine role in active team workspace from DB
                try {
                    Long uid = null;
                    String email = oidcUser != null ? oidcUser.getEmail() : null;
                    if (email != null) {
                        var userOpt = individualAccountRepository.findByEmail(email);
                        if (userOpt.isPresent()) uid = userOpt.get().getId();
                    }
                    if (uid != null && activeAccountId != null) {
                        activeRole = teamMemberRepository.findByTeamIdAndUserId(activeAccountId, uid)
                                .map(TeamMember::getRole)
                                .orElse(null);
                    }
                } catch (Exception ex) {
                    activeRole = null;
                }
                model.addAttribute("activeRole", activeRole);

                String normalizedRole = activeRole != null ? activeRole.toLowerCase(Locale.ROOT) : null;
                boolean canManageMembers = isTeamActive && ("owner".equals(normalizedRole) || "admin".equals(normalizedRole));
                boolean canSendDocuments = !isTeamActive || (normalizedRole == null || !"viewer".equals(normalizedRole));
                boolean canInviteMembers = !isTeamActive || canManageMembers;
                model.addAttribute("canManageMembers", canManageMembers);
                model.addAttribute("canSendDocuments", canSendDocuments);
                model.addAttribute("canInviteMembers", canInviteMembers);
                model.addAttribute("hasLimitedAccess", isTeamActive && !canSendDocuments);

                if (isTeamActive && activeAccountId != null) {
                    var activities = teamActivityService.getRecentActivity(activeAccountId, 15);
                    model.addAttribute("teamActivities", activities);
                } else {
                    model.addAttribute("teamActivities", List.of());
                }

                List<TeamMember> activeTeamMembers = List.of();
                if (isTeamActive && activeAccountId != null) {
                    try {
                        var members = teamMemberRepository.findByTeamId(activeAccountId);
                        if (members != null) {
                            activeTeamMembers = members.stream()
                                    .filter(tm -> tm != null && (tm.getStatus() == null || !"removed".equalsIgnoreCase(tm.getStatus())))
                                    .toList();
                        }
                    } catch (Exception ignored) {}
                }
                model.addAttribute("activeTeamMembers", activeTeamMembers);

                // Individual account code has been removed in the new schema.

                // Load document stats and recent documents from DB for the chosen account
                Long docsAccountId = activeAccountId != null
                        ? activeAccountId
                        : (personal != null ? personal.getId() : null);
                if (docsAccountId != null) {
                    long pending = documentRepository.countByAccountIdAndStatus(docsAccountId, "PENDING");
                    long completed = documentRepository.countByAccountIdAndStatus(docsAccountId, "COMPLETED");
                    long draft = documentRepository.countByAccountIdAndStatus(docsAccountId, "DRAFT");

                    model.addAttribute("pendingCount", pending);
                    model.addAttribute("completedCount", completed);
                    model.addAttribute("draftCount", draft);

                    var recent = documentRepository.findByAccountIdOrderByLastModifiedDesc(docsAccountId,
                            org.springframework.data.domain.PageRequest.of(0, 10));
                    model.addAttribute("recentDocuments", recent);
                }
            } else {
                model.addAttribute("myAccounts", List.of());
                model.addAttribute("personalAccount", null);
                model.addAttribute("teamAccounts", List.of());
                model.addAttribute("activeAccountId", null);
                model.addAttribute("activeRole", null);
                model.addAttribute("isTeamActive", false);
                model.addAttribute("teamActivities", List.of());
                model.addAttribute("activeTeamMembers", List.of());
                teamViewActive = false;
            }

            model.addAttribute("notifications", notifications);
            model.addAttribute("notificationCount", notifications != null ? notifications.size() : 0);

            logger.info("Successfully prepared dashboard model");
            return teamViewActive ? "team-dashboard" : "dashboard";

        } catch (Exception e) {
            logger.error("Error in dashboard endpoint", e);
            throw e;
        }
    }

    private Long getActiveAccountIdFromSession() {
        try {
            Object sessionVal = RequestContextHolder.currentRequestAttributes()
                    .getAttribute(ActiveAccountInterceptor.SESSION_KEY, RequestAttributes.SCOPE_SESSION);

            if (sessionVal instanceof Long l) return l;
            if (sessionVal instanceof String s) return Long.parseLong(s);

        } catch (Exception ignored) {
        }
        return null;
    }

    private String getSubject(Authentication authentication) {
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof org.springframework.security.oauth2.jwt.Jwt jwt) {
            return jwt.getSubject();
        }
        if (principal instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        return authentication.getName();
    }

    public static class NotificationItem {
        private final String status;
        private final String message;
        private final String detail;
        private final java.time.Instant timestamp;
        private final String email;

        private NotificationItem(String status, String message, String detail, java.time.Instant timestamp, String email) {
            this.status = status;
            this.message = message;
            this.detail = detail;
            this.timestamp = timestamp;
            this.email = email;
        }

        public static NotificationItem from(InviteToken token, String teamName) {
            if (token == null) {
                return null;
            }
            String nameOrEmail = token.getFullName() != null && !token.getFullName().isBlank()
                    ? token.getFullName()
                    : token.getEmail();
            boolean accepted = token.getAcceptedAt() != null;
            String status = accepted ? "accepted" : (token.isUsed() ? "completed" : "pending");
            StringBuilder message = new StringBuilder();
            if (accepted) {
                message.append(nameOrEmail != null ? nameOrEmail : "Invitation").append(" accepted your invite");
            } else {
                message.append("Invite sent to ").append(nameOrEmail != null ? nameOrEmail : "recipient");
            }
            if (teamName != null && !teamName.isBlank()) {
                message.append(" for ").append(teamName);
            }
            String detail = accepted && token.getRole() != null
                    ? "Role: " + token.getRole()
                    : null;
            java.time.Instant timestamp = accepted ? token.getAcceptedAt() : token.getCreatedAt();
            return new NotificationItem(status, message.toString(), detail, timestamp, token.getEmail());
        }

        public String getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public String getDetail() {
            return detail;
        }

        public java.time.Instant getTimestamp() {
            return timestamp;
        }

        public String getEmail() {
            return email;
        }
    }
}
