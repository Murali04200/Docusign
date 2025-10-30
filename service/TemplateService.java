package com.example.Docusign.service;

import com.example.Docusign.model.Template;
import com.example.Docusign.repository.TemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class TemplateService {

    private static final Logger log = LoggerFactory.getLogger(TemplateService.class);
    private static final String UPLOAD_DIR = "uploads/templates/";
    private static final String THUMBNAIL_DIR = "uploads/thumbnails/";

    private final TemplateRepository templateRepository;

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
        initializeDirectories();
    }

    private void initializeDirectories() {
        try {
            Files.createDirectories(Paths.get(UPLOAD_DIR));
            Files.createDirectories(Paths.get(THUMBNAIL_DIR));
        } catch (IOException e) {
            log.error("Failed to create upload directories", e);
        }
    }

    /**
     * Get all templates for an account, including default templates
     */
    public List<Template> getAllTemplates(Long accountId) {
        if (accountId == null) {
            return new java.util.ArrayList<>();
        }

        // Get user's custom templates
        List<Template> templates = templateRepository.findByAccountIdOrderByCreatedAtDesc(accountId);

        // Return empty list instead of null
        if (templates == null) {
            templates = new java.util.ArrayList<>();
        }

        // Add default system templates if not already present
        if (templates.isEmpty()) {
            templates.addAll(getDefaultTemplates(accountId));
        }

        return templates;
    }

    /**
     * Get default system templates
     */
    public List<Template> getDefaultTemplates(Long accountId) {
        return List.of(
            createDefaultTemplate(accountId, "Non-Disclosure Agreement (NDA)",
                "Protect confidential information shared between parties",
                "Legal", "nda-template.pdf"),

            createDefaultTemplate(accountId, "Employment Contract",
                "Standard employment agreement template with customizable terms",
                "HR", "employment-contract.pdf"),

            createDefaultTemplate(accountId, "Sales Agreement",
                "Professional sales contract for products or services",
                "Sales", "sales-agreement.pdf"),

            createDefaultTemplate(accountId, "Service Agreement",
                "Service provider agreement with terms and conditions",
                "Business", "service-agreement.pdf"),

            createDefaultTemplate(accountId, "Purchase Order",
                "Standard purchase order form for procurement",
                "Business", "purchase-order.pdf"),

            createDefaultTemplate(accountId, "Invoice Template",
                "Professional invoice template for billing",
                "Finance", "invoice-template.pdf"),

            createDefaultTemplate(accountId, "Rental Agreement",
                "Residential or commercial property rental agreement",
                "Real Estate", "rental-agreement.pdf"),

            createDefaultTemplate(accountId, "Consulting Agreement",
                "Independent contractor consulting agreement",
                "Professional Services", "consulting-agreement.pdf"),

            createDefaultTemplate(accountId, "Partnership Agreement",
                "Business partnership agreement template",
                "Legal", "partnership-agreement.pdf"),

            createDefaultTemplate(accountId, "Consent Form",
                "General consent and authorization form",
                "Legal", "consent-form.pdf")
        );
    }

    private Template createDefaultTemplate(Long accountId, String name, String description,
                                          String category, String fileName) {
        Template template = new Template();
        template.setId(UUID.randomUUID().toString());
        template.setAccountId(accountId);
        template.setName(name);
        template.setDescription(description);
        template.setCategory(category);
        template.setFilePath("defaults/" + fileName);
        template.setFileType("PDF");
        template.setThumbnailPath("defaults/thumbnails/" + fileName.replace(".pdf", ".png"));
        template.setDefault(true);
        template.setShared(false);
        template.setUsageCount(0);
        return template;
    }

    /**
     * Upload a new template
     */
    public Template uploadTemplate(Long accountId, String name, String description,
                                   String category, MultipartFile file) throws IOException {
        String fileId = UUID.randomUUID().toString();
        String originalFileName = file.getOriginalFilename();
        String fileExtension = originalFileName != null && originalFileName.contains(".")
            ? originalFileName.substring(originalFileName.lastIndexOf("."))
            : "";

        String fileName = fileId + fileExtension;
        Path filePath = Paths.get(UPLOAD_DIR + fileName);

        // Save file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Create template entity
        Template template = new Template();
        template.setId(fileId);
        template.setAccountId(accountId);
        template.setName(name);
        template.setDescription(description);
        template.setCategory(category);
        template.setFilePath(fileName);
        template.setFileType(getFileType(fileExtension));
        template.setDefault(false);
        template.setShared(false);

        return templateRepository.save(template);
    }

    /**
     * Get template by ID
     */
    public Optional<Template> getTemplate(String id, Long accountId) {
        return templateRepository.findByIdAndAccountId(id, accountId);
    }

    /**
     * Use a template (increment usage count)
     */
    public Template useTemplate(String id, Long accountId) {
        Optional<Template> templateOpt = templateRepository.findById(id);
        if (templateOpt.isPresent()) {
            Template template = templateOpt.get();
            template.incrementUsageCount();
            return templateRepository.save(template);
        }
        return null;
    }

    /**
     * Delete a template
     */
    public boolean deleteTemplate(String id, Long accountId) {
        Optional<Template> templateOpt = templateRepository.findByIdAndAccountId(id, accountId);
        if (templateOpt.isPresent()) {
            Template template = templateOpt.get();

            // Don't delete default templates
            if (template.isDefault()) {
                return false;
            }

            // Delete file
            try {
                Path filePath = Paths.get(UPLOAD_DIR + template.getFilePath());
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                log.error("Failed to delete template file", e);
            }

            templateRepository.delete(template);
            return true;
        }
        return false;
    }

    /**
     * Search templates
     */
    public List<Template> searchTemplates(Long accountId, String query) {
        if (accountId == null) {
            return new java.util.ArrayList<>();
        }

        if (query == null || query.isBlank()) {
            return getAllTemplates(accountId);
        }

        List<Template> results = templateRepository.findByAccountIdAndNameContainingIgnoreCase(accountId, query);
        return results != null ? results : new java.util.ArrayList<>();
    }

    /**
     * Get template count
     */
    public long getTemplateCount(Long accountId) {
        return templateRepository.countByAccountId(accountId);
    }

    /**
     * Initialize default templates for a new account
     */
    public void initializeDefaultTemplates(Long accountId) {
        long count = templateRepository.countByAccountId(accountId);
        if (count == 0) {
            List<Template> defaults = getDefaultTemplates(accountId);
            templateRepository.saveAll(defaults);
            log.info("Initialized {} default templates for account {}", defaults.size(), accountId);
        }
    }

    private String getFileType(String extension) {
        return switch (extension.toLowerCase()) {
            case ".pdf" -> "PDF";
            case ".docx", ".doc" -> "DOCX";
            case ".xlsx", ".xls" -> "XLSX";
            case ".txt" -> "TXT";
            default -> "UNKNOWN";
        };
    }
}
