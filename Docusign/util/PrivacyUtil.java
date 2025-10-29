package com.example.Docusign.util;

/**
 * Utility class for protecting user privacy
 * Provides methods to mask sensitive personal information
 */
public class PrivacyUtil {

    private PrivacyUtil() {
        // Utility class - prevent instantiation
    }

    /**
     * Masks an email address for privacy
     * Example: "john.doe@example.com" becomes "j***@example.com"
     *
     * @param email The email to mask
     * @return Masked email or null if input is null
     */
    public static String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }

        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            // Invalid email format, return as-is or mask entirely
            return "***";
        }

        String localPart = email.substring(0, atIndex);
        String domainPart = email.substring(atIndex);

        if (localPart.length() == 1) {
            return localPart + "***" + domainPart;
        } else if (localPart.length() == 2) {
            return localPart.charAt(0) + "*" + domainPart;
        } else {
            // Show first char and last char, mask the middle
            return localPart.charAt(0) + "***" + localPart.charAt(localPart.length() - 1) + domainPart;
        }
    }

    /**
     * Masks a name for privacy
     * Example: "John Doe" becomes "J*** D***"
     *
     * @param name The name to mask
     * @return Masked name or null if input is null
     */
    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }

        String[] parts = name.trim().split("\\s+");
        StringBuilder masked = new StringBuilder();

        for (int i = 0; i < parts.length; i++) {
            String part = parts[i];
            if (part.length() > 0) {
                masked.append(part.charAt(0)).append("***");
                if (i < parts.length - 1) {
                    masked.append(" ");
                }
            }
        }

        return masked.toString();
    }

    /**
     * Checks if a viewer has permission to see full member details
     * Only owners and admins can see unmasked emails
     *
     * @param viewerRole The role of the person viewing
     * @return true if viewer can see full details
     */
    public static boolean canViewFullMemberDetails(String viewerRole) {
        if (viewerRole == null) {
            return false;
        }
        return "owner".equalsIgnoreCase(viewerRole) || "admin".equalsIgnoreCase(viewerRole);
    }

    /**
     * Determines if email should be masked based on viewer role
     *
     * @param viewerRole Role of the person viewing the data
     * @param targetUserId User ID being viewed
     * @param viewerUserId User ID of the viewer
     * @return true if email should be masked
     */
    public static boolean shouldMaskEmail(String viewerRole, Long targetUserId, Long viewerUserId) {
        // Users can always see their own email
        if (targetUserId != null && targetUserId.equals(viewerUserId)) {
            return false;
        }

        // Owners and admins can see all emails
        if (canViewFullMemberDetails(viewerRole)) {
            return false;
        }

        // All other cases: mask the email
        return true;
    }
}
