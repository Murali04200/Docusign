package com.example.Docusign.team.model;

import java.time.Instant;

import jakarta.persistence.*;
import com.example.Docusign.account.model.IndividualAccount;

@Entity
@Table(name = "teams", indexes = {
        @Index(name = "idx_teams_code", columnList = "team_code", unique = true)
})
public class Team {

    @Id
    @Column(name = "owner_id")
    private Long id; // Primary key equals owner's IndividualAccount.id

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "team_code", nullable = false, unique = true)
    private String teamCode;

    // Share PK with owner so team.id == owner.id
    @OneToOne(optional = false)
    @MapsId
    @JoinColumn(name = "owner_id", nullable = false)
    private IndividualAccount owner; // PK and FK to individual_accounts.id

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getTeamCode() { return teamCode; }
    public void setTeamCode(String teamCode) { this.teamCode = teamCode; }
    public IndividualAccount getOwner() { return owner; }
    public void setOwner(IndividualAccount owner) { this.owner = owner; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
