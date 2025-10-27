package com.example.Docusign.invite;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "invite_tokens")
public class InviteToken {
    @Id
    private String id; // UUID
    private String token; // secure UUID token
    private String fullName;
    private String email;
    private String role; // Admin/Member/Viewer
    @Column(name = "account_id")
    private Long accountId; // target account to join
    private Instant createdAt;
    private Instant expiresAt;
    private boolean used;
    // who sent
    @Column(name = "inviter_user_id")
    private String inviterUserId;
    private String inviterName;
    private String inviterEmail;
    // acceptance tracking
    private Instant acceptedAt;
    @Column(name = "accepted_by_user_id")
    private String acceptedByUserId;

    public static InviteToken create(String fullName, String email, String role, Instant expiresAt) {
        InviteToken it = new InviteToken();
        it.id = UUID.randomUUID().toString();
        it.token = UUID.randomUUID().toString();
        it.fullName = fullName;
        it.email = email;
        it.role = role;
        it.createdAt = Instant.now();
        it.expiresAt = expiresAt;
        it.used = false;
        return it;
    }

    public String getId() { return id; }
    public String getToken() { return token; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Long getAccountId() { return accountId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isUsed() { return used; }

    public void setUsed(boolean used) { this.used = used; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getInviterUserId() { return inviterUserId; }
    public void setInviterUserId(String inviterUserId) { this.inviterUserId = inviterUserId; }
    public String getInviterName() { return inviterName; }
    public void setInviterName(String inviterName) { this.inviterName = inviterName; }
    public String getInviterEmail() { return inviterEmail; }
    public void setInviterEmail(String inviterEmail) { this.inviterEmail = inviterEmail; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
    public String getAcceptedByUserId() { return acceptedByUserId; }
    public void setAcceptedByUserId(String acceptedByUserId) { this.acceptedByUserId = acceptedByUserId; }
}
