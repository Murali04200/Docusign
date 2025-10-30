package com.example.Docusign.model;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "envelopes", indexes = {
    @Index(name = "idx_envelopes_account", columnList = "account_id"),
    @Index(name = "idx_envelopes_status", columnList = "status")
})
public class Envelope {

    @Id
    private String id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "name")
    private String name;

    @Column(name = "status", nullable = false)
    private String status = "draft";

    @Column(name = "subject")
    private String subject;

    @Lob
    @Column(name = "message")
    private String message;

    @Column(name = "sender_user_id")
    private String senderUserId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;

    @Column(name = "decline_reason", columnDefinition = "TEXT")
    private String declineReason;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "auto_reminder_enabled", nullable = false)
    private Boolean autoReminderEnabled = true;

    @Column(name = "reminder_frequency_days")
    private Integer reminderFrequencyDays = 3;

    @Column(name = "reminder_count", nullable = false)
    private Integer reminderCount = 0;

    @Column(name = "max_reminders")
    private Integer maxReminders = 3;

    @Column(name = "last_reminder_sent_at")
    private Instant lastReminderSentAt;

    @Column(name = "certificate_path", length = 500)
    private String certificatePath;

    @Column(name = "is_favorite", nullable = false)
    private Boolean isFavorite = false;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getSenderUserId() { return senderUserId; }
    public void setSenderUserId(String senderUserId) { this.senderUserId = senderUserId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant completedAt) { this.completedAt = completedAt; }
    public Instant getVoidedAt() { return voidedAt; }
    public void setVoidedAt(Instant voidedAt) { this.voidedAt = voidedAt; }
    public String getVoidReason() { return voidReason; }
    public void setVoidReason(String voidReason) { this.voidReason = voidReason; }
    public String getDeclineReason() { return declineReason; }
    public void setDeclineReason(String declineReason) { this.declineReason = declineReason; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Boolean getAutoReminderEnabled() { return autoReminderEnabled; }
    public void setAutoReminderEnabled(Boolean autoReminderEnabled) { this.autoReminderEnabled = autoReminderEnabled; }
    public Integer getReminderFrequencyDays() { return reminderFrequencyDays; }
    public void setReminderFrequencyDays(Integer reminderFrequencyDays) { this.reminderFrequencyDays = reminderFrequencyDays; }
    public Integer getReminderCount() { return reminderCount; }
    public void setReminderCount(Integer reminderCount) { this.reminderCount = reminderCount; }
    public Integer getMaxReminders() { return maxReminders; }
    public void setMaxReminders(Integer maxReminders) { this.maxReminders = maxReminders; }
    public Instant getLastReminderSentAt() { return lastReminderSentAt; }
    public void setLastReminderSentAt(Instant lastReminderSentAt) { this.lastReminderSentAt = lastReminderSentAt; }
    public String getCertificatePath() { return certificatePath; }
    public void setCertificatePath(String certificatePath) { this.certificatePath = certificatePath; }
    public Boolean getIsFavorite() { return isFavorite; }
    public void setIsFavorite(Boolean isFavorite) { this.isFavorite = isFavorite; }
}
