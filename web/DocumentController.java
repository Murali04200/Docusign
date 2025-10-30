package com.example.Docusign.web;

import com.example.Docusign.team.activity.TeamActivity;
import com.example.Docusign.team.activity.TeamActivityRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final TeamActivityRepository teamActivityRepository;

    public DocumentController(TeamActivityRepository teamActivityRepository) {
        this.teamActivityRepository = teamActivityRepository;
    }

    @GetMapping("/pending")
    public ResponseEntity<List<PendingDocument>> getPendingDocuments(@AuthenticationPrincipal OAuth2User principal) {
        try {
            String userEmail = principal.getAttribute("email");
            if (userEmail == null || userEmail.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            
            // Fetch activities that might contain document signing requests
            List<TeamActivity> activities = teamActivityRepository
                .findByActionTypeAndDetailContaining("DOCUMENT_FOR_SIGNATURE", userEmail);
                
            List<PendingDocument> pendingDocuments = activities.stream()
                .filter(activity -> {
                    // Extract document info from activity details
                    String details = activity.getDetail();
                    return details != null && details.contains(userEmail);
                })
                .map(activity -> {
                    // Parse document info from activity details
                    // Format: "DOCUMENT_FOR_SIGNATURE:documentId:documentName:recipientEmail"
                    String[] parts = activity.getDetail().split(":");
                    String documentId = parts.length > 1 ? parts[1] : "unknown";
                    String documentName = parts.length > 2 ? parts[2] : "Untitled Document";
                    
                    return new PendingDocument(
                        documentId,
                        documentName,
                        activity.getActorDisplayName() != null ? activity.getActorDisplayName() : "Unknown Sender",
                        activity.getActorEmail() != null ? activity.getActorEmail() : "No email provided",
                        activity.getCreatedAt()
                    );
                })
                .collect(Collectors.toList());
                
            return ResponseEntity.ok(pendingDocuments);
        } catch (Exception e) {
            // Log the error for debugging
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Represents a document that is pending signature by the current user
     */
    public static class PendingDocument {
        private final String documentId;
        private final String documentName;
        private final String senderName;
        private final String senderEmail;
        private final Instant sentAt;
        
        public PendingDocument(String documentId, String documentName, String senderName, String senderEmail, Instant sentAt) {
            this.documentId = documentId != null ? documentId : "unknown";
            this.documentName = documentName != null ? documentName : "Untitled Document";
            this.senderName = senderName != null ? senderName : "Unknown Sender";
            this.senderEmail = senderEmail != null ? senderEmail : "No email provided";
            this.sentAt = sentAt != null ? sentAt : Instant.now();
        }
        
        // Getters with null-safety
        public String getDocumentId() { return documentId; }
        public String getDocumentName() { return documentName; }
        public String getSenderName() { return senderName; }
        public String getSenderEmail() { return senderEmail; }
        public Instant getSentAt() { return sentAt; }
    }
}
