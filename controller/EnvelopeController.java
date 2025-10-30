package com.example.Docusign.controller;

import com.example.Docusign.model.Envelope;
import com.example.Docusign.model.EnvelopeRecipient;
import com.example.Docusign.service.EnvelopeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/agreements")
public class EnvelopeController {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeController.class);

    private final EnvelopeService envelopeService;

    public EnvelopeController(EnvelopeService envelopeService) {
        this.envelopeService = envelopeService;
    }

    /**
     * Show agreements/envelopes page
     */
    @GetMapping
    public String agreementsPage(@RequestParam(value = "status", required = false) String status,
                                @RequestParam(value = "search", required = false) String search,
                                Authentication authentication,
                                Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Long accountId = getAccountId(authentication);
        String userId = getUserId(authentication);

        // Get envelopes based on filters
        List<Envelope> envelopes;
        if (search != null && !search.isBlank()) {
            envelopes = envelopeService.searchEnvelopes(accountId, search);
        } else if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            envelopes = envelopeService.getEnvelopesByStatus(accountId, status);
        } else {
            envelopes = envelopeService.getAllEnvelopes(accountId);
        }

        // Get statistics
        EnvelopeService.EnvelopeStats stats = envelopeService.getEnvelopeStats(accountId);

        model.addAttribute("envelopes", envelopes != null ? envelopes : List.of());
        model.addAttribute("selectedStatus", status != null ? status : "all");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("stats", stats);
        model.addAttribute("totalCount", envelopes != null ? envelopes.size() : 0);

        return "agreements";
    }

    /**
     * View envelope details
     */
    @GetMapping("/{id}")
    public String viewEnvelope(@PathVariable String id,
                              Authentication authentication,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Long accountId = getAccountId(authentication);
        Optional<Envelope> envelopeOpt = envelopeService.getEnvelope(id, accountId);

        if (envelopeOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Envelope not found");
            return "redirect:/agreements";
        }

        Envelope envelope = envelopeOpt.get();
        List<EnvelopeRecipient> recipients = envelopeService.getRecipients(id);

        model.addAttribute("envelope", envelope);
        model.addAttribute("recipients", recipients);

        return "envelope-details";
    }

    /**
     * Void envelope
     */
    @PostMapping("/{id}/void")
    public String voidEnvelope(@PathVariable String id,
                              @RequestParam(value = "reason", required = false) String reason,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            envelopeService.voidEnvelope(id, reason != null ? reason : "Voided by sender");
            redirectAttributes.addFlashAttribute("success", "Envelope voided successfully");
        } catch (Exception e) {
            log.error("Failed to void envelope", e);
            redirectAttributes.addFlashAttribute("error", "Failed to void envelope: " + e.getMessage());
        }

        return "redirect:/agreements";
    }

    /**
     * Delete envelope (drafts only)
     */
    @PostMapping("/{id}/delete")
    public String deleteEnvelope(@PathVariable String id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            Long accountId = getAccountId(authentication);
            boolean deleted = envelopeService.deleteEnvelope(id, accountId);

            if (deleted) {
                redirectAttributes.addFlashAttribute("success", "Envelope deleted successfully");
            } else {
                redirectAttributes.addFlashAttribute("error", "Envelope not found");
            }
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            log.error("Failed to delete envelope", e);
            redirectAttributes.addFlashAttribute("error", "Failed to delete envelope");
        }

        return "redirect:/agreements";
    }

    /**
     * Resend envelope
     */
    @PostMapping("/{id}/resend")
    public String resendEnvelope(@PathVariable String id,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            // TODO: Implement resend logic (send reminder emails)
            redirectAttributes.addFlashAttribute("success", "Reminder sent to recipients");
        } catch (Exception e) {
            log.error("Failed to resend envelope", e);
            redirectAttributes.addFlashAttribute("error", "Failed to send reminder");
        }

        return "redirect:/agreements/" + id;
    }

    /**
     * Get account ID from authentication
     */
    private Long getAccountId(Authentication authentication) {
        if (authentication == null) {
            return 1L;
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return (long) Math.abs(oidcUser.getSubject().hashCode());
        }
        return 1L;
    }

    /**
     * Get user ID from authentication
     */
    private String getUserId(Authentication authentication) {
        if (authentication == null) {
            return "guest";
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getEmail() != null ? oidcUser.getEmail() : oidcUser.getSubject();
        }
        return authentication.getName();
    }
}
