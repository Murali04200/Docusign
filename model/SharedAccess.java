package com.example.Docusign.model;

import com.example.Docusign.account.model.IndividualAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "shared_access", indexes = {
        @Index(name = "idx_shared_envelope_id", columnList = "envelope_id"),
        @Index(name = "idx_shared_shared_with", columnList = "shared_with_account_id")
})
public class SharedAccess {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envelope_id", nullable = false, length = 100)
    private String envelopeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_by_account_id", nullable = false)
    private IndividualAccount sharedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shared_with_account_id", nullable = false)
    private IndividualAccount sharedWith;

    @Column(name = "permission_level", nullable = false, length = 50)
    private String permissionLevel = "view"; // view, edit, manage

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEnvelopeId() {
        return envelopeId;
    }

    public void setEnvelopeId(String envelopeId) {
        this.envelopeId = envelopeId;
    }

    public IndividualAccount getSharedBy() {
        return sharedBy;
    }

    public void setSharedBy(IndividualAccount sharedBy) {
        this.sharedBy = sharedBy;
    }

    public IndividualAccount getSharedWith() {
        return sharedWith;
    }

    public void setSharedWith(IndividualAccount sharedWith) {
        this.sharedWith = sharedWith;
    }

    public String getPermissionLevel() {
        return permissionLevel;
    }

    public void setPermissionLevel(String permissionLevel) {
        this.permissionLevel = permissionLevel;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }
}
