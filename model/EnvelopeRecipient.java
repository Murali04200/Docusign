package com.example.Docusign.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "envelope_recipients", indexes = {
    @Index(name = "idx_env_rec_envelope", columnList = "envelope_id"),
    @Index(name = "idx_env_rec_status", columnList = "status")
})
public class EnvelopeRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envelope_id", nullable = false)
    private String envelopeId; // FK -> envelopes.id

    @Column(name = "name")
    private String name;

    @Column(name = "email")
    private String email;

    @Column(name = "role", nullable = false)
    private String role = "signer"; // signer, cc, approver

    @Column(name = "routing_order")
    private Integer routingOrder = 1;

    @Column(name = "status", nullable = false)
    private String status = "pending"; // pending, delivered, completed, declined

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "viewed_at")
    private Instant viewedAt;

    @Column(name = "signed_at")
    private Instant signedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getEnvelopeId() { return envelopeId; }
    public void setEnvelopeId(String envelopeId) { this.envelopeId = envelopeId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public Integer getRoutingOrder() { return routingOrder; }
    public void setRoutingOrder(Integer routingOrder) { this.routingOrder = routingOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getDeliveredAt() { return deliveredAt; }
    public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public Instant getViewedAt() { return viewedAt; }
    public void setViewedAt(Instant viewedAt) { this.viewedAt = viewedAt; }
    public Instant getSignedAt() { return signedAt; }
    public void setSignedAt(Instant signedAt) { this.signedAt = signedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
