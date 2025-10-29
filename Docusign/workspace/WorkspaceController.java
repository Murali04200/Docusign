package com.example.Docusign.workspace;

import com.example.Docusign.account.dto.AccountResponse;
import com.example.Docusign.account.service.AccountService;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import com.example.Docusign.team.repository.TeamMemberRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class WorkspaceController {

    private final AccountService accountService;
    private final IndividualAccountRepository individualAccountRepository;
    private final TeamMemberRepository teamMemberRepository;

    public WorkspaceController(AccountService accountService,
                               IndividualAccountRepository individualAccountRepository,
                               TeamMemberRepository teamMemberRepository) {
        this.accountService = accountService;
        this.individualAccountRepository = individualAccountRepository;
        this.teamMemberRepository = teamMemberRepository;
    }

    @PostMapping("/workspace/switch")
    public String switchWorkspace(@RequestParam("accountId") Long accountId,
                                  Authentication authentication,
                                  HttpServletRequest request,
                                  RedirectAttributes redirectAttributes) {
        // Validate the user is a member of the requested account
        List<AccountResponse> mine = accountService.listMyAccounts(authentication);
        boolean allowed = mine.stream().anyMatch(a -> a.getId().equals(accountId));
        if (!allowed) {
            redirectAttributes.addFlashAttribute("message", "You don't have access to this workspace.");
            return "redirect:/dashboard";
        }
        // Additional validation via team membership table by email
        String email = null;
        if (authentication != null && authentication.getPrincipal() instanceof org.springframework.security.oauth2.core.oidc.user.OidcUser oidc) {
            email = oidc.getEmail();
        }
        String role = null;
        Long currentUserId = null;
        boolean isPersonalWorkspace = false;
        if (email != null) {
            var user = individualAccountRepository.findByEmail(email);
            if (user.isPresent()) {
                currentUserId = user.get().getId();
                isPersonalWorkspace = currentUserId != null && accountId.equals(currentUserId);
                if (!isPersonalWorkspace && currentUserId != null) {
                    var tm = teamMemberRepository.findByTeamIdAndUserId(accountId, currentUserId);
                    if (tm.isEmpty()) {
                        redirectAttributes.addFlashAttribute("message", "You are not a member of this team.");
                        return "redirect:/dashboard";
                    }
                    role = tm.get().getRole();
                } else if (isPersonalWorkspace) {
                    role = "owner";
                }
            }
        }
        request.getSession(true).setAttribute(ActiveAccountInterceptor.SESSION_KEY, accountId);
        // Prepare a friendly message with workspace name and role
        String workspaceName = mine.stream()
                .filter(a -> a.getId().equals(accountId))
                .map(AccountResponse::getName)
                .findFirst().orElse("Workspace");
        String suffix = role != null ? (" as " + role) : "";
        String msg = "Switched to " + workspaceName + suffix + ".";
        redirectAttributes.addFlashAttribute("message", msg);
        return "redirect:/dashboard";
    }

    @PostMapping("/workspace/leave")
    public String leaveWorkspace(Authentication authentication,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        Object activeObj = request.getSession(false) != null ? request.getSession(false).getAttribute(ActiveAccountInterceptor.SESSION_KEY) : null;
        Long accountId = null;
        if (activeObj instanceof Long l) accountId = l; else if (activeObj instanceof String s) { try { accountId = Long.parseLong(s);} catch (Exception ignored) {} }
        if (accountId == null) {
            redirectAttributes.addFlashAttribute("message", "No active workspace to leave.");
            return "redirect:/dashboard";
        }
        // Clear active workspace
        if (request.getSession(false) != null) {
            request.getSession(false).removeAttribute(ActiveAccountInterceptor.SESSION_KEY);
        }
        redirectAttributes.addFlashAttribute("message", "You have left the team.");
        return "redirect:/dashboard";
    }
}

