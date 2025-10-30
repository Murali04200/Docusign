package com.example.Docusign.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "document_history", indexes = {
        @Index(name = "idx_doc_history_envelope_id", columnList = "envelope_id"),
        @Index(name = "idx_doc_history_created_at", columnList = "created_at")
})
public class DocumentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envelope_id", nullable = false, length = 100)
    private String envelopeId;

    @Column(name = "action", nullable = false, length = 100)
    private String action; // created, sent, viewed, signed, declined, voided, completed, downloaded, reminded, expired

    @Column(name = "actor_name", length = 200)
    private String actorName;

    @Column(name = "actor_email", length = 255)
    private String actorEmail;

    @Column(name = "actor_ip_address", length = 50)
    private String actorIpAddress;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON format for additional data

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getActorName() {
        return actorName;
    }

    public void setActorName(String actorName) {
        this.actorName = actorName;
    }

    public String getActorEmail() {
        return actorEmail;
    }

    public void setActorEmail(String actorEmail) {
        this.actorEmail = actorEmail;
    }

    public String getActorIpAddress() {
        return actorIpAddress;
    }

    public void setActorIpAddress(String actorIpAddress) {
        this.actorIpAddress = actorIpAddress;
    }

    public String getDetails() {
        return details;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
