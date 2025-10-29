package com.example.Docusign.team.dto;

import java.time.Instant;

/**
 * Privacy-safe Team Member Response DTO
 * Email addresses are masked by default to protect personal account privacy.
 * Only team owners/admins should see full emails.
 */
public class TeamMemberResponse {

    private Long id;
    private Long userId;
    private String displayName;
    private String email; // Can be masked depending on viewer's role
    private String role;
    private String status;
    private Instant joinedAt;
    private Instant createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getJoinedAt() {
        return joinedAt;
    }

    public void setJoinedAt(Instant joinedAt) {
        this.joinedAt = joinedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Masks the email for privacy protection
     * Example: "user@example.com" becomes "u***@example.com"
     */
    public void maskEmail() {
        if (this.email != null && this.email.contains("@")) {
            String[] parts = this.email.split("@");
            if (parts.length == 2 && parts[0].length() > 0) {
                String masked = parts[0].charAt(0) + "***@" + parts[1];
                this.email = masked;
            }
        }
    }
}
