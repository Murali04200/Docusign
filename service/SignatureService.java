package com.example.Docusign.service;

import com.example.Docusign.account.model.IndividualAccount;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import com.example.Docusign.model.Signature;
import com.example.Docusign.repository.SignatureRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SignatureService {

    private static final Logger log = LoggerFactory.getLogger(SignatureService.class);

    private final SignatureRepository signatureRepository;
    private final IndividualAccountRepository accountRepository;

    @Value("${app.upload.signatures.dir:uploads/signatures}")
    private String uploadDir;

    public SignatureService(SignatureRepository signatureRepository, IndividualAccountRepository accountRepository) {
        this.signatureRepository = signatureRepository;
        this.accountRepository = accountRepository;
    }

    /**
     * Get all signatures for a user account
     */
    public List<Signature> getSignaturesByAccount(IndividualAccount account) {
        return signatureRepository.findByAccountOrderByCreatedAtDesc(account);
    }

    /**
     * Get all signatures by account ID
     */
    public List<Signature> getSignaturesByAccountId(Long accountId) {
        return signatureRepository.findByAccountId(accountId);
    }

    /**
     * Get default signature for an account
     */
    public Optional<Signature> getDefaultSignature(IndividualAccount account) {
        return signatureRepository.findByAccountAndIsDefaultTrue(account);
    }

    /**
     * Get signature by ID with security check (must belong to account)
     */
    public Optional<Signature> getSignatureById(Long id, IndividualAccount account) {
        return signatureRepository.findByIdAndAccount(id, account);
    }

    /**
     * Create a drawn signature from canvas data
     */
    @Transactional
    public Signature createDrawnSignature(IndividualAccount account, String signatureName,
                                         String canvasData, Integer width, Integer height,
                                         boolean setAsDefault) throws IOException {

        // Save canvas data as image file
        String imagePath = saveBase64Image(canvasData, account.getId());

        Signature signature = new Signature();
        signature.setAccount(account);
        signature.setSignatureName(signatureName != null ? signatureName : "My Signature");
        signature.setSignatureType(Signature.SignatureType.DRAW);
        signature.setSignatureData(canvasData);
        signature.setImagePath(imagePath);
        signature.setWidth(width);
        signature.setHeight(height);
        signature.setIsDefault(setAsDefault);

        // If setting as default, clear other defaults
        if (setAsDefault) {
            clearDefaultSignatures(account.getId());
        }

        Signature saved = signatureRepository.save(signature);
        log.info("Created drawn signature {} for account {}", saved.getId(), account.getId());

        return saved;
    }

    /**
     * Create a typed signature
     */
    @Transactional
    public Signature createTypedSignature(IndividualAccount account, String signatureName,
                                         String typedText, String fontStyle,
                                         boolean setAsDefault) {

        Signature signature = new Signature();
        signature.setAccount(account);
        signature.setSignatureName(signatureName != null ? signatureName : "My Typed Signature");
        signature.setSignatureType(Signature.SignatureType.TYPE);
        signature.setSignatureData(typedText);
        signature.setFontStyle(fontStyle);
        signature.setIsDefault(setAsDefault);

        // If setting as default, clear other defaults
        if (setAsDefault) {
            clearDefaultSignatures(account.getId());
        }

        Signature saved = signatureRepository.save(signature);
        log.info("Created typed signature {} for account {}", saved.getId(), account.getId());

        return saved;
    }

    /**
     * Create an uploaded signature from image file
     */
    @Transactional
    public Signature createUploadedSignature(IndividualAccount account, String signatureName,
                                            MultipartFile file, boolean setAsDefault) throws IOException {

        // Validate file
        validateImageFile(file);

        // Save uploaded file
        String imagePath = saveUploadedFile(file, account.getId());

        // Get image dimensions
        BufferedImage img = ImageIO.read(file.getInputStream());
        int width = img.getWidth();
        int height = img.getHeight();

        Signature signature = new Signature();
        signature.setAccount(account);
        signature.setSignatureName(signatureName != null ? signatureName : "Uploaded Signature");
        signature.setSignatureType(Signature.SignatureType.UPLOAD);
        signature.setImagePath(imagePath);
        signature.setWidth(width);
        signature.setHeight(height);
        signature.setIsDefault(setAsDefault);

        // If setting as default, clear other defaults
        if (setAsDefault) {
            clearDefaultSignatures(account.getId());
        }

        Signature saved = signatureRepository.save(signature);
        log.info("Created uploaded signature {} for account {}", saved.getId(), account.getId());

        return saved;
    }

    /**
     * Create initials signature
     */
    @Transactional
    public Signature createInitialsSignature(IndividualAccount account, String signatureName,
                                            String initialsData, String fontStyle,
                                            Signature.SignatureType type, boolean setAsDefault) throws IOException {

        Signature signature = new Signature();
        signature.setAccount(account);
        signature.setSignatureName(signatureName != null ? signatureName : "My Initials");
        signature.setSignatureType(Signature.SignatureType.INITIAL);
        signature.setSignatureData(initialsData);
        signature.setFontStyle(fontStyle);
        signature.setIsDefault(setAsDefault);

        // If it's drawn initials, save as image
        if (type == Signature.SignatureType.DRAW && initialsData.startsWith("data:image")) {
            String imagePath = saveBase64Image(initialsData, account.getId());
            signature.setImagePath(imagePath);
        }

        // If setting as default, clear other defaults
        if (setAsDefault) {
            clearDefaultSignatures(account.getId());
        }

        Signature saved = signatureRepository.save(signature);
        log.info("Created initials signature {} for account {}", saved.getId(), account.getId());

        return saved;
    }

    /**
     * Update signature name
     */
    @Transactional
    public Signature updateSignatureName(Long signatureId, IndividualAccount account, String newName) {
        Signature signature = signatureRepository.findByIdAndAccount(signatureId, account)
                .orElseThrow(() -> new IllegalArgumentException("Signature not found"));

        signature.setSignatureName(newName);
        return signatureRepository.save(signature);
    }

    /**
     * Set signature as default
     */
    @Transactional
    public Signature setAsDefault(Long signatureId, IndividualAccount account) {
        Signature signature = signatureRepository.findByIdAndAccount(signatureId, account)
                .orElseThrow(() -> new IllegalArgumentException("Signature not found"));

        // Clear all other defaults for this account
        clearDefaultSignatures(account.getId());

        // Set this signature as default
        signature.setIsDefault(true);
        Signature saved = signatureRepository.save(signature);

        log.info("Set signature {} as default for account {}", signatureId, account.getId());
        return saved;
    }

    /**
     * Delete signature
     */
    @Transactional
    public void deleteSignature(Long signatureId, IndividualAccount account) {
        Signature signature = signatureRepository.findByIdAndAccount(signatureId, account)
                .orElseThrow(() -> new IllegalArgumentException("Signature not found"));

        // Delete associated image file if exists
        if (signature.getImagePath() != null) {
            deleteImageFile(signature.getImagePath());
        }

        signatureRepository.delete(signature);
        log.info("Deleted signature {} for account {}", signatureId, account.getId());
    }

    /**
     * Clear all default signatures for an account
     */
    @Transactional
    public void clearDefaultSignatures(Long accountId) {
        signatureRepository.clearDefaultSignatures(accountId);
    }

    /**
     * Count signatures for an account
     */
    public long countSignatures(IndividualAccount account) {
        return signatureRepository.countByAccount(account);
    }

    // ==================== Private Helper Methods ====================

    /**
     * Save base64 image data to file system
     */
    private String saveBase64Image(String base64Data, Long accountId) throws IOException {
        // Remove data:image/png;base64, prefix if present
        String imageData = base64Data;
        if (base64Data.contains(",")) {
            imageData = base64Data.split(",")[1];
        }

        // Decode base64
        byte[] imageBytes = Base64.getDecoder().decode(imageData);

        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDir, accountId.toString());
        Files.createDirectories(uploadPath);

        // Generate unique filename
        String filename = UUID.randomUUID().toString() + ".png";
        Path filePath = uploadPath.resolve(filename);

        // Write file
        Files.write(filePath, imageBytes);

        // Return relative path
        return accountId + "/" + filename;
    }

    /**
     * Save uploaded file to file system
     */
    private String saveUploadedFile(MultipartFile file, Long accountId) throws IOException {
        // Create directory if not exists
        Path uploadPath = Paths.get(uploadDir, accountId.toString());
        Files.createDirectories(uploadPath);

        // Get file extension
        String originalFilename = file.getOriginalFilename();
        String extension = originalFilename != null && originalFilename.contains(".")
                ? originalFilename.substring(originalFilename.lastIndexOf("."))
                : ".png";

        // Generate unique filename
        String filename = UUID.randomUUID().toString() + extension;
        Path filePath = uploadPath.resolve(filename);

        // Copy file
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        // Return relative path
        return accountId + "/" + filename;
    }

    /**
     * Delete image file from file system
     */
    private void deleteImageFile(String relativePath) {
        try {
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
            log.info("Deleted signature image file: {}", relativePath);
        } catch (IOException e) {
            log.error("Error deleting signature image file: {}", relativePath, e);
        }
    }

    /**
     * Validate uploaded image file
     */
    private void validateImageFile(MultipartFile file) {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        // Check file size (max 5MB)
        long maxSize = 5 * 1024 * 1024; // 5MB
        if (file.getSize() > maxSize) {
            throw new IllegalArgumentException("File size exceeds 5MB limit");
        }

        // Check content type
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("File must be an image");
        }

        // Allowed image types
        List<String> allowedTypes = List.of("image/png", "image/jpeg", "image/jpg", "image/gif");
        if (!allowedTypes.contains(contentType.toLowerCase())) {
            throw new IllegalArgumentException("Only PNG, JPEG, JPG, and GIF images are allowed");
        }
    }
}
