package com.example.Docusign.model;

import com.example.Docusign.account.model.IndividualAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "bulk_sends", indexes = {
        @Index(name = "idx_bulk_send_account_id", columnList = "account_id"),
        @Index(name = "idx_bulk_send_status", columnList = "status")
})
public class BulkSend {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private IndividualAccount account;

    @Column(name = "bulk_name", nullable = false, length = 200)
    private String bulkName;

    @Column(name = "template_id", length = 100)
    private String templateId;

    @Column(name = "status", nullable = false, length = 50)
    private String status = "draft"; // draft, processing, completed, failed

    @Column(name = "total_recipients", nullable = false)
    private Integer totalRecipients = 0;

    @Column(name = "sent_count", nullable = false)
    private Integer sentCount = 0;

    @Column(name = "failed_count", nullable = false)
    private Integer failedCount = 0;

    @Column(name = "csv_file_path", length = 500)
    private String csvFilePath;

    @Column(name = "subject_template", length = 500)
    private String subjectTemplate;

    @Column(name = "message_template", columnDefinition = "TEXT")
    private String messageTemplate;

    @Column(name = "error_log", columnDefinition = "TEXT")
    private String errorLog;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

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

    public String getBulkName() {
        return bulkName;
    }

    public void setBulkName(String bulkName) {
        this.bulkName = bulkName;
    }

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getTotalRecipients() {
        return totalRecipients;
    }

    public void setTotalRecipients(Integer totalRecipients) {
        this.totalRecipients = totalRecipients;
    }

    public Integer getSentCount() {
        return sentCount;
    }

    public void setSentCount(Integer sentCount) {
        this.sentCount = sentCount;
    }

    public Integer getFailedCount() {
        return failedCount;
    }

    public void setFailedCount(Integer failedCount) {
        this.failedCount = failedCount;
    }

    public String getCsvFilePath() {
        return csvFilePath;
    }

    public void setCsvFilePath(String csvFilePath) {
        this.csvFilePath = csvFilePath;
    }

    public String getSubjectTemplate() {
        return subjectTemplate;
    }

    public void setSubjectTemplate(String subjectTemplate) {
        this.subjectTemplate = subjectTemplate;
    }

    public String getMessageTemplate() {
        return messageTemplate;
    }

    public void setMessageTemplate(String messageTemplate) {
        this.messageTemplate = messageTemplate;
    }

    public String getErrorLog() {
        return errorLog;
    }

    public void setErrorLog(String errorLog) {
        this.errorLog = errorLog;
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

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }
}
