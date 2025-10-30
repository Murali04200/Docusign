package com.example.Docusign.model;

import com.example.Docusign.account.model.IndividualAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;

@Entity
@Table(name = "envelope_comments", indexes = {
        @Index(name = "idx_comment_envelope_id", columnList = "envelope_id"),
        @Index(name = "idx_comment_account_id", columnList = "account_id")
})
public class EnvelopeComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "envelope_id", nullable = false, length = 100)
    private String envelopeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private IndividualAccount account;

    @Column(name = "comment_text", columnDefinition = "TEXT", nullable = false)
    private String commentText;

    @Column(name = "commenter_name", length = 200)
    private String commenterName;

    @Column(name = "commenter_email", length = 255)
    private String commenterEmail;

    @Column(name = "is_internal", nullable = false)
    private Boolean isInternal = true; // internal comments vs visible to recipients

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

    public IndividualAccount getAccount() {
        return account;
    }

    public void setAccount(IndividualAccount account) {
        this.account = account;
    }

    public String getCommentText() {
        return commentText;
    }

    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    public String getCommenterName() {
        return commenterName;
    }

    public void setCommenterName(String commenterName) {
        this.commenterName = commenterName;
    }

    public String getCommenterEmail() {
        return commenterEmail;
    }

    public void setCommenterEmail(String commenterEmail) {
        this.commenterEmail = commenterEmail;
    }

    public Boolean getIsInternal() {
        return isInternal;
    }

    public void setIsInternal(Boolean isInternal) {
        this.isInternal = isInternal;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
