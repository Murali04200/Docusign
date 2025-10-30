package com.example.Docusign.web;

import com.example.Docusign.model.Envelope;
import com.example.Docusign.model.EnvelopeRecipient;
import com.example.Docusign.repository.EnvelopeRecipientRepository;
import com.example.Docusign.repository.EnvelopeRepository;
import com.example.Docusign.service.EnvelopeService;
import com.example.Docusign.signature.EditorStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Controller
public class SigningController {

    private static final Logger log = LoggerFactory.getLogger(SigningController.class);

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeRecipientRepository recipientRepository;
    private final EnvelopeService envelopeService;
    private final EditorStore editorStore;

    public SigningController(EnvelopeRepository envelopeRepository,
                            EnvelopeRecipientRepository recipientRepository,
                            EnvelopeService envelopeService,
                            EditorStore editorStore) {
        this.envelopeRepository = envelopeRepository;
        this.recipientRepository = recipientRepository;
        this.envelopeService = envelopeService;
        this.editorStore = editorStore;
    }

    @GetMapping("/sign-document")
    public String signDocumentPage(@RequestParam("envelopeId") String envelopeId,
                                   @RequestParam("email") String email,
                                   Model model) {
        log.info("Recipient {} accessing envelope {} for signing", email, envelopeId);

        // Get envelope
        Optional<Envelope> envelopeOpt = envelopeRepository.findById(envelopeId);
        if (envelopeOpt.isEmpty()) {
            model.addAttribute("error", "Document not found");
            return "error";
        }

        Envelope envelope = envelopeOpt.get();

        // Get recipient
        Optional<EnvelopeRecipient> recipientOpt = recipientRepository
            .findByEnvelopeIdAndEmail(envelopeId, email);

        if (recipientOpt.isEmpty()) {
            model.addAttribute("error", "You are not authorized to sign this document");
            return "error";
        }

        EnvelopeRecipient recipient = recipientOpt.get();

        // Check if already signed
        if ("completed".equals(recipient.getStatus())) {
            model.addAttribute("message", "You have already signed this document");
            model.addAttribute("envelope", envelope);
            model.addAttribute("recipient", recipient);
            return "sign-document-completed";
        }

        // Check if it's recipient's turn (routing order)
        List<EnvelopeRecipient> allRecipients = recipientRepository
            .findByEnvelopeIdOrderByRoutingOrderAsc(envelopeId);

        boolean isRecipientTurn = checkIfRecipientTurn(recipient, allRecipients);
        if (!isRecipientTurn) {
            model.addAttribute("error", "It's not your turn to sign yet. Previous recipients must sign first.");
            return "error";
        }

        // Mark as viewed if not already
        if (recipient.getViewedAt() == null) {
            recipient.setViewedAt(Instant.now());
            recipientRepository.save(recipient);
        }

        // Get all recipients for display
        model.addAttribute("envelope", envelope);
        model.addAttribute("recipient", recipient);
        model.addAttribute("allRecipients", allRecipients);
        model.addAttribute("documentName", envelope.getName());
        model.addAttribute("senderName", envelope.getSenderUserId());

        return "sign-document";
    }

    @GetMapping("/sign-document/file")
    public ResponseEntity<FileSystemResource> getDocumentFile(@RequestParam("envelopeId") String envelopeId,
                                                              @RequestParam("email") String email) {
        // Verify recipient access
        Optional<EnvelopeRecipient> recipientOpt = recipientRepository
            .findByEnvelopeIdAndEmail(envelopeId, email);

        if (recipientOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        // Check if temp envelope exists in EditorStore
        EditorStore.TempEnvelope tempEnv = editorStore.get(envelopeId);
        if (tempEnv != null && tempEnv.filePath != null) {
            FileSystemResource resource = new FileSystemResource(tempEnv.filePath);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + tempEnv.fileName + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
        }

        // Try to get from permanent storage
        Path documentPath = Paths.get("uploads/envelopes/" + envelopeId + ".pdf");
        if (Files.exists(documentPath)) {
            FileSystemResource resource = new FileSystemResource(documentPath);
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"document.pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping("/sign-document/placements")
    @ResponseBody
    public ResponseEntity<String> getFieldPlacements(@RequestParam("envelopeId") String envelopeId) {
        EditorStore.TempEnvelope tempEnv = editorStore.get(envelopeId);
        if (tempEnv != null && tempEnv.placementsJson != null) {
            return ResponseEntity.ok(tempEnv.placementsJson);
        }
        return ResponseEntity.ok("[]");
    }

    @PostMapping("/sign-document/submit")
    @ResponseBody
    public ResponseEntity<?> submitSignature(@RequestParam("envelopeId") String envelopeId,
                                             @RequestParam("email") String email,
                                             @RequestParam("signatureData") String signatureData) {
        try {
            log.info("Recipient {} submitting signature for envelope {}", email, envelopeId);

            // Get recipient
            Optional<EnvelopeRecipient> recipientOpt = recipientRepository
                .findByEnvelopeIdAndEmail(envelopeId, email);

            if (recipientOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Recipient not found");
            }

            EnvelopeRecipient recipient = recipientOpt.get();

            // Mark as signed
            envelopeService.markRecipientSigned(envelopeId, email);

            log.info("Recipient {} successfully signed envelope {}", email, envelopeId);

            // Check if envelope is completed
            Optional<Envelope> envelopeOpt = envelopeRepository.findById(envelopeId);
            if (envelopeOpt.isPresent()) {
                Envelope envelope = envelopeOpt.get();
                return ResponseEntity.ok()
                    .body(new SignResponse(true, "Document signed successfully!", envelope.getStatus()));
            }

            return ResponseEntity.ok()
                .body(new SignResponse(true, "Document signed successfully!", "sent"));

        } catch (Exception e) {
            log.error("Error submitting signature", e);
            return ResponseEntity.badRequest()
                .body(new SignResponse(false, "Failed to sign document: " + e.getMessage(), null));
        }
    }

    @PostMapping("/sign-document/decline")
    @ResponseBody
    public ResponseEntity<?> declineSignature(@RequestParam("envelopeId") String envelopeId,
                                              @RequestParam("email") String email,
                                              @RequestParam("reason") String reason) {
        try {
            log.info("Recipient {} declining envelope {}: {}", email, envelopeId, reason);

            // Get recipient
            Optional<EnvelopeRecipient> recipientOpt = recipientRepository
                .findByEnvelopeIdAndEmail(envelopeId, email);

            if (recipientOpt.isEmpty()) {
                return ResponseEntity.badRequest().body("Recipient not found");
            }

            EnvelopeRecipient recipient = recipientOpt.get();
            recipient.setStatus("declined");
            recipient.setDeclinedAt(Instant.now());
            recipientRepository.save(recipient);

            // Update envelope status
            Optional<Envelope> envelopeOpt = envelopeRepository.findById(envelopeId);
            if (envelopeOpt.isPresent()) {
                Envelope envelope = envelopeOpt.get();
                envelope.setStatus("declined");
                envelope.setDeclineReason(reason);
                envelopeRepository.save(envelope);
            }

            return ResponseEntity.ok()
                .body(new SignResponse(true, "Document declined", "declined"));

        } catch (Exception e) {
            log.error("Error declining document", e);
            return ResponseEntity.badRequest()
                .body(new SignResponse(false, "Failed to decline document: " + e.getMessage(), null));
        }
    }

    /**
     * Check if it's recipient's turn based on routing order
     */
    private boolean checkIfRecipientTurn(EnvelopeRecipient recipient, List<EnvelopeRecipient> allRecipients) {
        int recipientOrder = recipient.getRoutingOrder();

        // Check if all previous recipients have completed
        for (EnvelopeRecipient r : allRecipients) {
            if (r.getRoutingOrder() < recipientOrder) {
                if (!"completed".equals(r.getStatus()) && "signer".equals(r.getRole())) {
                    return false; // Previous signer hasn't completed
                }
            }
        }

        return true;
    }

    /**
     * Response DTO for sign/decline operations
     */
    public static class SignResponse {
        private boolean success;
        private String message;
        private String envelopeStatus;

        public SignResponse(boolean success, String message, String envelopeStatus) {
            this.success = success;
            this.message = message;
            this.envelopeStatus = envelopeStatus;
        }

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getEnvelopeStatus() {
            return envelopeStatus;
        }

        public void setEnvelopeStatus(String envelopeStatus) {
            this.envelopeStatus = envelopeStatus;
        }
    }
}
