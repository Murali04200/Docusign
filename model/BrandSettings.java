package com.example.Docusign.model;

import com.example.Docusign.account.model.IndividualAccount;
import jakarta.persistence.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "brand_settings")
public class BrandSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private IndividualAccount account;

    @Column(name = "company_name", length = 200)
    private String companyName;

    @Column(name = "logo_path", length = 500)
    private String logoPath;

    @Column(name = "primary_color", length = 7)
    private String primaryColor = "#6366f1";

    @Column(name = "secondary_color", length = 7)
    private String secondaryColor = "#8b5cf6";

    @Column(name = "email_subject_template", length = 500)
    private String emailSubjectTemplate;

    @Column(name = "email_message_template", columnDefinition = "TEXT")
    private String emailMessageTemplate;

    @Column(name = "show_company_logo", nullable = false)
    private Boolean showCompanyLogo = true;

    @Column(name = "custom_email_footer", columnDefinition = "TEXT")
    private String customEmailFooter;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "support_email", length = 255)
    private String supportEmail;

    @Column(name = "support_phone", length = 50)
    private String supportPhone;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

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

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getLogoPath() {
        return logoPath;
    }

    public void setLogoPath(String logoPath) {
        this.logoPath = logoPath;
    }

    public String getPrimaryColor() {
        return primaryColor;
    }

    public void setPrimaryColor(String primaryColor) {
        this.primaryColor = primaryColor;
    }

    public String getSecondaryColor() {
        return secondaryColor;
    }

    public void setSecondaryColor(String secondaryColor) {
        this.secondaryColor = secondaryColor;
    }

    public String getEmailSubjectTemplate() {
        return emailSubjectTemplate;
    }

    public void setEmailSubjectTemplate(String emailSubjectTemplate) {
        this.emailSubjectTemplate = emailSubjectTemplate;
    }

    public String getEmailMessageTemplate() {
        return emailMessageTemplate;
    }

    public void setEmailMessageTemplate(String emailMessageTemplate) {
        this.emailMessageTemplate = emailMessageTemplate;
    }

    public Boolean getShowCompanyLogo() {
        return showCompanyLogo;
    }

    public void setShowCompanyLogo(Boolean showCompanyLogo) {
        this.showCompanyLogo = showCompanyLogo;
    }

    public String getCustomEmailFooter() {
        return customEmailFooter;
    }

    public void setCustomEmailFooter(String customEmailFooter) {
        this.customEmailFooter = customEmailFooter;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public String getSupportEmail() {
        return supportEmail;
    }

    public void setSupportEmail(String supportEmail) {
        this.supportEmail = supportEmail;
    }

    public String getSupportPhone() {
        return supportPhone;
    }

    public void setSupportPhone(String supportPhone) {
        this.supportPhone = supportPhone;
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
