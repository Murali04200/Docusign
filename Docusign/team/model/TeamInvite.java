package com.example.Docusign.team.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "team_invites", indexes = {
        @Index(name = "idx_team_invites_token", columnList = "token", unique = true)
})
public class TeamInvite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId; // FK to teams.id

    @Column(name = "invited_user_email", nullable = false)
    private String invitedUserEmail;

    @Column(name = "invited_user_id")
    private Long invitedUserId; // FK to individual_accounts.id (nullable)

    @Column(name = "invited_by")
    private Long invitedBy; // FK to individual_accounts.id (nullable)

    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "status", nullable = false)
    private String status = "pending"; // 'pending','accepted','expired','revoked'

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "accepted_at")
    private Instant acceptedAt;

    public Long getId() { return id; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getInvitedUserEmail() { return invitedUserEmail; }
    public void setInvitedUserEmail(String invitedUserEmail) { this.invitedUserEmail = invitedUserEmail; }
    public Long getInvitedUserId() { return invitedUserId; }
    public void setInvitedUserId(Long invitedUserId) { this.invitedUserId = invitedUserId; }
    public Long getInvitedBy() { return invitedBy; }
    public void setInvitedBy(Long invitedBy) { this.invitedBy = invitedBy; }
    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(Instant acceptedAt) { this.acceptedAt = acceptedAt; }
}
