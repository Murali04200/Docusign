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
@Table(name = "templates", indexes = {
    @Index(name = "idx_templates_account", columnList = "account_id")
})
public class Template {

    @Id
    private String id; // UUID/external id

    @Column(name = "account_id", nullable = false)
    private Long accountId; // FK -> individual_accounts.id

    @Column(name = "name", nullable = false)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "category")
    private String category; // e.g., "Business", "Legal", "HR", "Sales", "Real Estate"

    @Column(name = "file_path")
    private String filePath; // Path to stored template file

    @Column(name = "file_type")
    private String fileType; // PDF, DOCX, etc.

    @Column(name = "thumbnail_path")
    private String thumbnailPath; // Preview image

    @Column(name = "is_default")
    private boolean isDefault = false; // System default templates

    @Column(name = "is_shared")
    private boolean isShared = false; // Shared with team

    @Column(name = "usage_count")
    private int usageCount = 0; // How many times used

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getAccountId() { return accountId; }
    public void setAccountId(Long accountId) { this.accountId = accountId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public String getThumbnailPath() { return thumbnailPath; }
    public void setThumbnailPath(String thumbnailPath) { this.thumbnailPath = thumbnailPath; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public boolean isShared() { return isShared; }
    public void setShared(boolean shared) { isShared = shared; }
    public int getUsageCount() { return usageCount; }
    public void setUsageCount(int usageCount) { this.usageCount = usageCount; }
    public void incrementUsageCount() { this.usageCount++; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
