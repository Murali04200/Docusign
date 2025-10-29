package com.example.Docusign.controller;

import com.example.Docusign.model.Template;
import com.example.Docusign.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/templates")
public class TemplateController {

    private static final Logger log = LoggerFactory.getLogger(TemplateController.class);

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    /**
     * Show templates page
     */
    @GetMapping
    public String templatesPage(@RequestParam(value = "category", required = false) String category,
                               @RequestParam(value = "search", required = false) String search,
                               Authentication authentication,
                               Model model) {
        if (authentication == null) {
            return "redirect:/login";
        }

        // Get active account ID (simplified - in production, get from session/context)
        Long accountId = getAccountId(authentication);

        // Get templates
        List<Template> templates;
        if (search != null && !search.isBlank()) {
            templates = templateService.searchTemplates(accountId, search);
        } else {
            templates = templateService.getAllTemplates(accountId);
        }

        // Filter by category if specified
        if (category != null && !category.isBlank() && !"All".equalsIgnoreCase(category)) {
            templates = templates.stream()
                .filter(t -> category.equalsIgnoreCase(t.getCategory()))
                .collect(Collectors.toList());
        }

        // Group templates by category
        Map<String, List<Template>> templatesByCategory = templates.stream()
            .collect(Collectors.groupingBy(
                t -> t.getCategory() != null ? t.getCategory() : "Other"
            ));

        // Get unique categories
        List<String> categories = templates.stream()
            .map(Template::getCategory)
            .filter(c -> c != null && !c.isBlank())
            .distinct()
            .sorted()
            .collect(Collectors.toList());

        // Ensure all model attributes are non-null
        model.addAttribute("templates", templates != null ? templates : new java.util.ArrayList<>());
        model.addAttribute("templatesByCategory", templatesByCategory != null ? templatesByCategory : new java.util.HashMap<>());
        model.addAttribute("categories", categories != null ? categories : new java.util.ArrayList<>());
        model.addAttribute("selectedCategory", category != null && !category.isBlank() ? category : "All");
        model.addAttribute("searchQuery", search != null ? search : "");
        model.addAttribute("totalCount", templates != null ? templates.size() : 0);

        return "templates";
    }

    /**
     * Upload new template
     */
    @PostMapping("/upload")
    public String uploadTemplate(@RequestParam("templateName") String name,
                                @RequestParam(value = "templateDescription", required = false) String description,
                                @RequestParam("templateCategory") String category,
                                @RequestParam("templateFile") MultipartFile file,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Please select a file to upload");
            return "redirect:/templates";
        }

        try {
            Long accountId = getAccountId(authentication);
            Template template = templateService.uploadTemplate(accountId, name, description, category, file);

            redirectAttributes.addFlashAttribute("success",
                "Template '" + template.getName() + "' uploaded successfully!");
            log.info("Template uploaded: {} by account {}", template.getName(), accountId);

        } catch (IOException e) {
            log.error("Failed to upload template", e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to upload template: " + e.getMessage());
        }

        return "redirect:/templates";
    }

    /**
     * Use a template
     */
    @PostMapping("/{id}/use")
    public String useTemplate(@PathVariable String id,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            Long accountId = getAccountId(authentication);
            Template template = templateService.useTemplate(id, accountId);

            if (template != null) {
                redirectAttributes.addFlashAttribute("success",
                    "Template '" + template.getName() + "' is ready to use!");
                return "redirect:/send-signature?templateId=" + id;
            } else {
                redirectAttributes.addFlashAttribute("error", "Template not found");
                return "redirect:/templates";
            }
        } catch (Exception e) {
            log.error("Failed to use template", e);
            redirectAttributes.addFlashAttribute("error", "Failed to use template: " + e.getMessage());
            return "redirect:/templates";
        }
    }

    /**
     * Delete template
     */
    @PostMapping("/{id}/delete")
    public String deleteTemplate(@PathVariable String id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        Long accountId = getAccountId(authentication);
        boolean deleted = templateService.deleteTemplate(id, accountId);

        if (deleted) {
            redirectAttributes.addFlashAttribute("success", "Template deleted successfully");
        } else {
            redirectAttributes.addFlashAttribute("error",
                "Failed to delete template. Default templates cannot be deleted.");
        }

        return "redirect:/templates";
    }

    /**
     * Initialize default templates for account
     */
    @PostMapping("/initialize-defaults")
    public String initializeDefaults(Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        if (authentication == null) {
            return "redirect:/login";
        }

        try {
            Long accountId = getAccountId(authentication);
            templateService.initializeDefaultTemplates(accountId);
            redirectAttributes.addFlashAttribute("success",
                "Default templates initialized successfully!");
        } catch (Exception e) {
            log.error("Failed to initialize default templates", e);
            redirectAttributes.addFlashAttribute("error",
                "Failed to initialize default templates");
        }

        return "redirect:/templates";
    }

    /**
     * Get account ID from authentication
     * TODO: Integrate with proper account/workspace context
     */
    private Long getAccountId(Authentication authentication) {
        // Simplified - in production, get from active workspace context
        // For now, return a default ID based on user
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            // Use subject hash as account ID for demo
            return (long) Math.abs(oidcUser.getSubject().hashCode());
        }
        return 1L; // Default account
    }
}
