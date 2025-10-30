package com.example.Docusign.controller;

import com.example.Docusign.account.model.IndividualAccount;
import com.example.Docusign.account.repository.IndividualAccountRepository;
import com.example.Docusign.model.Signature;
import com.example.Docusign.service.SignatureService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/signatures")
public class SignatureApiController {

    private static final Logger log = LoggerFactory.getLogger(SignatureApiController.class);

    private final SignatureService signatureService;
    private final IndividualAccountRepository accountRepository;

    public SignatureApiController(SignatureService signatureService, IndividualAccountRepository accountRepository) {
        this.signatureService = signatureService;
        this.accountRepository = accountRepository;
    }

    /**
     * Get all signatures for the authenticated user
     */
    @GetMapping
    public ResponseEntity<?> getAllSignatures(Authentication authentication) {
        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            List<Signature> signatures = signatureService.getSignaturesByAccount(account);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "signatures", signatures,
                    "count", signatures.size()
            ));

        } catch (Exception e) {
            log.error("Error fetching signatures", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch signatures: " + e.getMessage()));
        }
    }

    /**
     * Get default signature
     */
    @GetMapping("/default")
    public ResponseEntity<?> getDefaultSignature(Authentication authentication) {
        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Optional<Signature> signature = signatureService.getDefaultSignature(account);

            if (signature.isPresent()) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "signature", signature.get()
                ));
            } else {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "signature", null,
                        "message", "No default signature set"
                ));
            }

        } catch (Exception e) {
            log.error("Error fetching default signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to fetch default signature: " + e.getMessage()));
        }
    }

    /**
     * Create a drawn signature
     */
    @PostMapping("/draw")
    public ResponseEntity<?> createDrawnSignature(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String signatureName = (String) request.get("signatureName");
            String canvasData = (String) request.get("canvasData");
            Integer width = (Integer) request.get("width");
            Integer height = (Integer) request.get("height");
            Boolean setAsDefault = (Boolean) request.getOrDefault("setAsDefault", false);

            if (canvasData == null || canvasData.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Canvas data is required"));
            }

            Signature signature = signatureService.createDrawnSignature(
                    account, signatureName, canvasData, width, height, setAsDefault);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Signature created successfully",
                            "signature", signature
                    ));

        } catch (IOException e) {
            log.error("Error saving drawn signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save signature: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating drawn signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create signature: " + e.getMessage()));
        }
    }

    /**
     * Create a typed signature
     */
    @PostMapping("/type")
    public ResponseEntity<?> createTypedSignature(
            @RequestBody Map<String, Object> request,
            Authentication authentication) {

        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String signatureName = (String) request.get("signatureName");
            String typedText = (String) request.get("typedText");
            String fontStyle = (String) request.get("fontStyle");
            Boolean setAsDefault = (Boolean) request.getOrDefault("setAsDefault", false);

            if (typedText == null || typedText.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Typed text is required"));
            }

            Signature signature = signatureService.createTypedSignature(
                    account, signatureName, typedText, fontStyle, setAsDefault);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Typed signature created successfully",
                            "signature", signature
                    ));

        } catch (Exception e) {
            log.error("Error creating typed signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create typed signature: " + e.getMessage()));
        }
    }

    /**
     * Create an uploaded signature
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createUploadedSignature(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "signatureName", required = false) String signatureName,
            @RequestParam(value = "setAsDefault", defaultValue = "false") boolean setAsDefault,
            Authentication authentication) {

        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "File is required"));
            }

            Signature signature = signatureService.createUploadedSignature(
                    account, signatureName, file, setAsDefault);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            "success", true,
                            "message", "Signature uploaded successfully",
                            "signature", signature
                    ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (IOException e) {
            log.error("Error saving uploaded signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to save signature: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error creating uploaded signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create signature: " + e.getMessage()));
        }
    }

    /**
     * Set signature as default
     */
    @PutMapping("/{id}/set-default")
    public ResponseEntity<?> setAsDefault(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            Signature signature = signatureService.setAsDefault(id, account);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Signature set as default",
                    "signature", signature
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error setting signature as default", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to set signature as default: " + e.getMessage()));
        }
    }

    /**
     * Update signature name
     */
    @PutMapping("/{id}/name")
    public ResponseEntity<?> updateSignatureName(
            @PathVariable Long id,
            @RequestBody Map<String, String> request,
            Authentication authentication) {

        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            String newName = request.get("signatureName");
            if (newName == null || newName.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Signature name is required"));
            }

            Signature signature = signatureService.updateSignatureName(id, account, newName);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Signature name updated",
                    "signature", signature
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error updating signature name", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update signature name: " + e.getMessage()));
        }
    }

    /**
     * Delete signature
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSignature(
            @PathVariable Long id,
            Authentication authentication) {

        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            signatureService.deleteSignature(id, account);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Signature deleted successfully"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Error deleting signature", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete signature: " + e.getMessage()));
        }
    }

    /**
     * Get signature count
     */
    @GetMapping("/count")
    public ResponseEntity<?> getSignatureCount(Authentication authentication) {
        try {
            IndividualAccount account = getAuthenticatedAccount(authentication);
            if (account == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "User not authenticated"));
            }

            long count = signatureService.countSignatures(account);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "count", count
            ));

        } catch (Exception e) {
            log.error("Error counting signatures", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to count signatures: " + e.getMessage()));
        }
    }

    // ==================== Helper Methods ====================

    /**
     * Get authenticated account from OAuth2/OIDC or JWT token
     */
    private IndividualAccount getAuthenticatedAccount(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return null;
        }

        String email = null;

        // Handle OAuth2 login (OidcUser from browser login)
        if (authentication.getPrincipal() instanceof OidcUser) {
            OidcUser oidcUser = (OidcUser) authentication.getPrincipal();
            email = oidcUser.getEmail();

            if (email == null) {
                email = oidcUser.getPreferredUsername();
            }

            if (email == null) {
                email = oidcUser.getClaimAsString("email");
            }
        }
        // Handle JWT token (API access)
        else if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            email = jwt.getClaimAsString("email");

            if (email == null) {
                email = jwt.getClaimAsString("preferred_username");
            }
        }

        if (email == null) {
            return null;
        }

        return accountRepository.findByEmail(email).orElse(null);
    }
}
