package com.example.Docusign.model;

import com.example.Docusign.account.model.IndividualAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "signatures", indexes = {
        @Index(name = "idx_signature_account_id", columnList = "account_id"),
        @Index(name = "idx_signature_is_default", columnList = "account_id, is_default")
})
public class Signature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private IndividualAccount account;

    @Column(name = "signature_name", length = 100)
    private String signatureName;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 20)
    private SignatureType signatureType;

    @Column(name = "signature_data", columnDefinition = "TEXT")
    private String signatureData;

    @Column(name = "image_path", length = 500)
    private String imagePath;

    @Column(name = "font_style", length = 50)
    private String fontStyle;

    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "width")
    private Integer width;

    @Column(name = "height")
    private Integer height;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public enum SignatureType {
        DRAW,      // Hand-drawn signature using canvas
        TYPE,      // Typed signature with font styling
        UPLOAD,    // Uploaded image signature
        INITIAL    // User initials
    }

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = Instant.now();
        }
        if (this.isDefault == null) {
            this.isDefault = false;
        }
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }

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

    public String getSignatureName() {
        return signatureName;
    }

    public void setSignatureName(String signatureName) {
        this.signatureName = signatureName;
    }

    public SignatureType getSignatureType() {
        return signatureType;
    }

    public void setSignatureType(SignatureType signatureType) {
        this.signatureType = signatureType;
    }

    public String getSignatureData() {
        return signatureData;
    }

    public void setSignatureData(String signatureData) {
        this.signatureData = signatureData;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getFontStyle() {
        return fontStyle;
    }

    public void setFontStyle(String fontStyle) {
        this.fontStyle = fontStyle;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
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
}
