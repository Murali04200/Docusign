package com.example.Docusign.team.model;

import java.time.Instant;

import jakarta.persistence.*;

@Entity
@Table(name = "team_members",
       uniqueConstraints = {
           @UniqueConstraint(name = "uniq_team_user", columnNames = {"team_id", "user_id"})
       })
public class TeamMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "team_id", nullable = false)
    private Long teamId; // FK to teams.id

    // Redundant but denormalized FK to team_code for quicker lookups and external references
    @Column(name = "team_code", nullable = false)
    private String teamCode; // FK to teams.team_code (unique)

    // Optional read-only association by team_code to enforce FK semantics
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_code", referencedColumnName = "team_code", insertable = false, updatable = false)
    private Team teamByCode;

    @Column(name = "user_id", nullable = false)
    private Long userId; // FK to individual_accounts.id

    @Column(name = "display_name")
    private String displayName;

    @Column(name = "email")
    private String email; // user email snapshot

    @Column(name = "role", nullable = false)
    private String role = "member"; // 'owner','admin','member','viewer'

    @Column(name = "status", nullable = false)
    private String status = "invited"; // 'invited','accepted','declined','removed'

    @Column(name = "invited_by")
    private Long invitedBy; // FK optional to individual_accounts.id

    @Column(name = "joined_at")
    private Instant joinedAt; // set when status = 'accepted'

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public Long getTeamId() { return teamId; }
    public void setTeamId(Long teamId) { this.teamId = teamId; }
    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getInvitedBy() { return invitedBy; }
    public void setInvitedBy(Long invitedBy) { this.invitedBy = invitedBy; }
    public Instant getJoinedAt() { return joinedAt; }
    public void setJoinedAt(Instant joinedAt) { this.joinedAt = joinedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
