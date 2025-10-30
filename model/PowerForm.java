package com.example.Docusign.model;

import com.example.Docusign.account.model.IndividualAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "power_forms", indexes = {
        @Index(name = "idx_power_form_account_id", columnList = "account_id"),
        @Index(name = "idx_power_form_unique_link", columnList = "unique_link", unique = true)
})
public class PowerForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private IndividualAccount account;

    @Column(name = "form_name", nullable = false, length = 200)
    private String formName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "template_id", length = 100)
    private String templateId;

    @Column(name = "unique_link", nullable = false, unique = true, length = 100)
    private String uniqueLink;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "require_authentication", nullable = false)
    private Boolean requireAuthentication = false;

    @Column(name = "allow_multiple_submissions", nullable = false)
    private Boolean allowMultipleSubmissions = true;

    @Column(name = "submissions_count", nullable = false)
    private Integer submissionsCount = 0;

    @Column(name = "max_submissions")
    private Integer maxSubmissions;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "success_message", columnDefinition = "TEXT")
    private String successMessage;

    @Column(name = "success_redirect_url", length = 500)
    private String successRedirectUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "last_submitted_at")
    private Instant lastSubmittedAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public IndividualAccount getAccount() {
        return account;
    }

    public void setAccount(IndividualAccount account) {
        this.account = account;
    }

    public String getFormName() {
        return formName;
    }

    public void setFormName(String formName) {
        this.formName = formName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getUniqueLink() {
        return uniqueLink;
    }

    public void setUniqueLink(String uniqueLink) {
        this.uniqueLink = uniqueLink;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getRequireAuthentication() {
        return requireAuthentication;
    }

    public void setRequireAuthentication(Boolean requireAuthentication) {
        this.requireAuthentication = requireAuthentication;
    }

    public Boolean getAllowMultipleSubmissions() {
        return allowMultipleSubmissions;
    }

    public void setAllowMultipleSubmissions(Boolean allowMultipleSubmissions) {
        this.allowMultipleSubmissions = allowMultipleSubmissions;
    }

    public Integer getSubmissionsCount() {
        return submissionsCount;
    }

    public void setSubmissionsCount(Integer submissionsCount) {
        this.submissionsCount = submissionsCount;
    }

    public Integer getMaxSubmissions() {
        return maxSubmissions;
    }

    public void setMaxSubmissions(Integer maxSubmissions) {
        this.maxSubmissions = maxSubmissions;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getSuccessMessage() {
        return successMessage;
    }

    public void setSuccessMessage(String successMessage) {
        this.successMessage = successMessage;
    }

    public String getSuccessRedirectUrl() {
        return successRedirectUrl;
    }

    public void setSuccessRedirectUrl(String successRedirectUrl) {
        this.successRedirectUrl = successRedirectUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getLastSubmittedAt() {
        return lastSubmittedAt;
    }

    public void setLastSubmittedAt(Instant lastSubmittedAt) {
        this.lastSubmittedAt = lastSubmittedAt;
    }
}
