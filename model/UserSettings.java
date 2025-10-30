package com.example.Docusign.model;

import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "user_settings")
public class UserSettings {

    @Id
    private String userId; // Same as user subject/email

    @Column(name = "email_notifications")
    private boolean emailNotifications = true;

    @Column(name = "reminder_frequency")
    private Integer reminderFrequency = 3; // days

    @Column(name = "signature_style")
    private String signatureStyle = "typed"; // typed, drawn, uploaded

    @Lob
    @Column(name = "signature_image_data")
    private String signatureImageData; // Base64 encoded image

    @Column(name = "default_expiration_days")
    private Integer defaultExpirationDays = 30;

    @Column(name = "language")
    private String language = "en";

    @Column(name = "timezone")
    private String timezone = "UTC";

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Getters and Setters
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public boolean isEmailNotifications() { return emailNotifications; }
    public void setEmailNotifications(boolean emailNotifications) { this.emailNotifications = emailNotifications; }

    public Integer getReminderFrequency() { return reminderFrequency; }
    public void setReminderFrequency(Integer reminderFrequency) { this.reminderFrequency = reminderFrequency; }

    public String getSignatureStyle() { return signatureStyle; }
    public void setSignatureStyle(String signatureStyle) { this.signatureStyle = signatureStyle; }

    public String getSignatureImageData() { return signatureImageData; }
    public void setSignatureImageData(String signatureImageData) { this.signatureImageData = signatureImageData; }

    public Integer getDefaultExpirationDays() { return defaultExpirationDays; }
    public void setDefaultExpirationDays(Integer defaultExpirationDays) { this.defaultExpirationDays = defaultExpirationDays; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getTimezone() { return timezone; }
    public void setTimezone(String timezone) { this.timezone = timezone; }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
