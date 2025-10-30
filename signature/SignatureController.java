package com.example.Docusign.signature;

import com.example.Docusign.model.Envelope;
import com.example.Docusign.model.EnvelopeRecipient;
import com.example.Docusign.model.Template;
import com.example.Docusign.service.EnvelopeService;
import com.example.Docusign.service.TemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Controller
public class SignatureController {

    private static final Logger log = LoggerFactory.getLogger(SignatureController.class);

    private final EditorStore editorStore;
    private final TemplateService templateService;
    private final EnvelopeService envelopeService;

    public SignatureController(EditorStore editorStore, TemplateService templateService, EnvelopeService envelopeService) {
        this.editorStore = editorStore;
        this.templateService = templateService;
        this.envelopeService = envelopeService;
    }

    @GetMapping("/send-signature")
    public String sendSignaturePage(@RequestParam(value = "templateId", required = false) String templateId,
                                   Authentication authentication,
                                   Model model) {
        // If template ID is provided, load the template
        if (templateId != null && !templateId.isBlank() && authentication != null) {
            Long accountId = getAccountId(authentication);
            Optional<Template> templateOpt = templateService.getTemplate(templateId, accountId);

            if (templateOpt.isPresent()) {
                Template template = templateOpt.get();
                model.addAttribute("template", template);
                model.addAttribute("templateId", templateId);
                model.addAttribute("preloadedFile", true);
            }
        }

        return "send-signature";
    }

    @PostMapping("/send-signature/next")
    public String sendSignatureNext(
            @RequestParam(name = "document", required = false) MultipartFile document,
            @RequestParam(name = "templateId", required = false) String templateId,
            @RequestParam(name = "recipientName", required = false) String[] recipientNames,
            @RequestParam(name = "recipientEmail", required = false) String[] recipientEmails,
            @RequestParam(name = "permission", required = false) String[] permissions,
            Authentication authentication,
            RedirectAttributes redirectAttributes
    ) throws IOException {
        Path tmp;
        String fileName;
        String contentType;

        // Check if using a template or uploading a document
        if ((document == null || document.isEmpty()) && (templateId != null && !templateId.isBlank())) {
            // Load template file
            Long accountId = getAccountId(authentication);
            Optional<Template> templateOpt = templateService.getTemplate(templateId, accountId);

            if (templateOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("message", "Template not found.");
                return "redirect:/send-signature";
            }

            Template template = templateOpt.get();
            String filePath = template.getFilePath();

            // Determine the actual file path
            Path templateFile;
            if (filePath.startsWith("defaults/")) {
                // Default templates are in resources (not implemented yet - would need classpath loading)
                // For now, redirect with error
                redirectAttributes.addFlashAttribute("message", "Default templates are not yet supported. Please upload a document.");
                return "redirect:/send-signature";
            } else {
                // Uploaded templates are in uploads/templates/
                templateFile = Paths.get("uploads/templates/" + filePath);
            }

            if (!Files.exists(templateFile)) {
                redirectAttributes.addFlashAttribute("message", "Template file not found.");
                return "redirect:/send-signature";
            }

            // Copy template to temp file
            String suffix = filePath.contains(".") ? filePath.substring(filePath.lastIndexOf('.')) : ".bin";
            tmp = Files.createTempFile("envelope-", suffix);
            Files.copy(templateFile, tmp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

            fileName = template.getName() + suffix;
            contentType = "application/" + template.getFileType().toLowerCase();

        } else if (document != null && !document.isEmpty()) {
            // Use uploaded document
            String original = document.getOriginalFilename();
            String suffix = original != null && original.contains(".") ? original.substring(original.lastIndexOf('.')) : ".bin";
            tmp = Files.createTempFile("envelope-", suffix);
            Files.write(tmp, document.getBytes());

            fileName = original != null ? original : "document";
            contentType = document.getContentType();

        } else {
            redirectAttributes.addFlashAttribute("message", "Please upload a document or select a template.");
            return "redirect:/send-signature";
        }

        // Map recipients
        List<EditorStore.Recipient> list = new ArrayList<>();
        if (recipientNames != null && recipientEmails != null && permissions != null) {
            int count = Math.min(recipientNames.length, Math.min(recipientEmails.length, permissions.length));
            for (int i = 0; i < count; i++) {
                EditorStore.Recipient r = new EditorStore.Recipient();
                r.name = recipientNames[i];
                r.email = recipientEmails[i];
                r.permission = permissions[i];
                list.add(r);
            }
        }
        EditorStore.Recipient[] arr = list.toArray(new EditorStore.Recipient[0]);

        String id = editorStore.save(tmp, fileName, contentType, arr);
        return "redirect:/send-signature/editor?id=" + id;
    }

    @GetMapping("/send-signature/editor")
    public String editorPage(@RequestParam("id") String id, Model model) {
        EditorStore.TempEnvelope env = editorStore.get(id);
        if (env == null) return "redirect:/send-signature";

        model.addAttribute("envId", id);
        model.addAttribute("fileName", env.fileName);
        model.addAttribute("recipients", env.recipients);

        // DocuSign recipient colors
        String[] recipientColors = {
            "#0d6efd", // Blue
            "#198754", // Green
            "#dc3545", // Red
            "#ffc107", // Yellow
            "#6f42c1", // Purple
            "#fd7e14", // Orange
            "#20c997", // Teal
            "#d63384"  // Pink
        };
        model.addAttribute("recipientColors", recipientColors);

        return "send-signature-editor";
    }

    @GetMapping("/send-signature/file")
    public ResponseEntity<FileSystemResource> getFile(@RequestParam("id") String id) {
        EditorStore.TempEnvelope env = editorStore.get(id);
        if (env == null || env.filePath == null) return ResponseEntity.notFound().build();
        FileSystemResource res = new FileSystemResource(env.filePath);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + env.fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(res);
    }

    @PostMapping("/send-signature/placements")
    public String savePlacements(@RequestParam("id") String id,
                                 @RequestParam("placements") String placements,
                                 RedirectAttributes redirectAttributes) {
        editorStore.setPlacements(id, placements);
        redirectAttributes.addFlashAttribute("message", "Fields saved.");
        return "redirect:/send-signature/editor?id=" + id;
    }

    @PostMapping("/send-signature/send")
    public String sendEnvelope(@RequestParam("id") String id,
                               @RequestParam("placements") String placements,
                               @RequestParam(value = "subject", required = false) String subject,
                               @RequestParam(value = "message", required = false) String message,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            editorStore.setPlacements(id, placements);

            // Get envelope data from temp storage
            EditorStore.TempEnvelope tempEnv = editorStore.get(id);
            if (tempEnv == null) {
                redirectAttributes.addFlashAttribute("error", "Envelope not found.");
                return "redirect:/send-signature";
            }

            // Get user info
            Long accountId = getAccountId(authentication);
            String senderUserId = getUserId(authentication);
            String senderName = getUserName(authentication);

            // Create envelope in database
            String envelopeName = tempEnv.fileName != null ? tempEnv.fileName : "Document";
            String envelopeSubject = subject != null && !subject.isBlank() ? subject : "Please review and sign: " + envelopeName;
            String envelopeMessage = message != null && !message.isBlank() ? message : "You have been requested to sign a document.";

            Envelope envelope = envelopeService.createEnvelope(accountId, senderUserId, envelopeName, envelopeSubject, envelopeMessage);
            log.info("Created envelope: {} for account: {}", envelope.getId(), accountId);

            // Add recipients with routing order
            if (tempEnv.recipients != null && tempEnv.recipients.length > 0) {
                for (int i = 0; i < tempEnv.recipients.length; i++) {
                    EditorStore.Recipient recipient = tempEnv.recipients[i];
                    String role = mapPermissionToRole(recipient.permission);
                    int routingOrder = i + 1; // Routing order starts at 1

                    envelopeService.addRecipient(
                        envelope.getId(),
                        recipient.name,
                        recipient.email,
                        role,
                        routingOrder
                    );
                    log.info("Added recipient: {} ({}) with routing order {} to envelope: {}",
                            recipient.name, recipient.email, routingOrder, envelope.getId());
                }
            }

            // Save document file permanently before sending
            try {
                Path envelopesDir = Paths.get("uploads/envelopes");
                if (!Files.exists(envelopesDir)) {
                    Files.createDirectories(envelopesDir);
                }

                // Copy temp file to permanent storage
                Path permanentPath = envelopesDir.resolve(envelope.getId() + ".pdf");
                Files.copy(tempEnv.filePath, permanentPath, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                log.info("Saved document permanently: {}", permanentPath);
            } catch (Exception e) {
                log.error("Failed to save document permanently", e);
            }

            // Send the envelope (updates status and sends notifications)
            envelope = envelopeService.sendEnvelope(envelope.getId());
            log.info("Sent envelope: {} with status: {}", envelope.getId(), envelope.getStatus());

            // DON'T clean up temp storage yet - keep it for signing page
            // editorStore.remove(id);
            // Instead, just keep the placements and file path for signing

            redirectAttributes.addFlashAttribute("success", "Agreement sent successfully!");
            return "redirect:/agreements";

        } catch (Exception e) {
            log.error("Failed to send envelope", e);
            redirectAttributes.addFlashAttribute("error", "Failed to send agreement: " + e.getMessage());
            return "redirect:/send-signature/editor?id=" + id;
        }
    }

    /**
     * Map permission to envelope recipient role
     */
    private String mapPermissionToRole(String permission) {
        if (permission == null) return "signer";
        return switch (permission.toLowerCase()) {
            case "sign", "signer" -> "signer";
            case "cc", "carbon copy" -> "cc";
            case "approve", "approver" -> "approver";
            default -> "signer";
        };
    }

    /**
     * Get user ID from authentication
     */
    private String getUserId(Authentication authentication) {
        if (authentication == null) {
            return "anonymous";
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            return oidcUser.getSubject();
        }
        return authentication.getName();
    }

    /**
     * Get user name from authentication
     */
    private String getUserName(Authentication authentication) {
        if (authentication == null) {
            return "Anonymous User";
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            String name = oidcUser.getFullName();
            if (name != null && !name.isBlank()) {
                return name;
            }
            String givenName = oidcUser.getGivenName();
            String familyName = oidcUser.getFamilyName();
            if (givenName != null && familyName != null) {
                return givenName + " " + familyName;
            }
            return oidcUser.getEmail() != null ? oidcUser.getEmail() : "User";
        }
        return authentication.getName();
    }

    /**
     * Get account ID from authentication
     */
    private Long getAccountId(Authentication authentication) {
        if (authentication == null) {
            return 1L; // Default account
        }
        if (authentication.getPrincipal() instanceof OidcUser oidcUser) {
            // Use subject hash as account ID for demo
            return (long) Math.abs(oidcUser.getSubject().hashCode());
        }
        return 1L; // Default account
    }
}
