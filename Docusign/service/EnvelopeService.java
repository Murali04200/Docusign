package com.example.Docusign.service;

import com.example.Docusign.model.Envelope;
import com.example.Docusign.model.EnvelopeRecipient;
import com.example.Docusign.model.Notification;
import com.example.Docusign.repository.EnvelopeRecipientRepository;
import com.example.Docusign.repository.EnvelopeRepository;
import com.example.Docusign.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional
public class EnvelopeService {

    private static final Logger log = LoggerFactory.getLogger(EnvelopeService.class);

    private final EnvelopeRepository envelopeRepository;
    private final EnvelopeRecipientRepository recipientRepository;
    private final NotificationRepository notificationRepository;

    public EnvelopeService(EnvelopeRepository envelopeRepository,
                          EnvelopeRecipientRepository recipientRepository,
                          NotificationRepository notificationRepository) {
        this.envelopeRepository = envelopeRepository;
        this.recipientRepository = recipientRepository;
        this.notificationRepository = notificationRepository;
    }

    /**
     * Create a new envelope
     */
    public Envelope createEnvelope(Long accountId, String senderUserId, String name,
                                   String subject, String message) {
        Envelope envelope = new Envelope();
        envelope.setId(UUID.randomUUID().toString());
        envelope.setAccountId(accountId);
        envelope.setSenderUserId(senderUserId);
        envelope.setName(name);
        envelope.setSubject(subject);
        envelope.setMessage(message);
        envelope.setStatus("draft");

        envelope = envelopeRepository.save(envelope);
        log.info("Created envelope {} for account {}", envelope.getId(), accountId);

        return envelope;
    }

    /**
     * Add recipient to envelope
     */
    public EnvelopeRecipient addRecipient(String envelopeId, String name, String email,
                                          String role, Integer routingOrder) {
        // Check if recipient already exists
        Optional<EnvelopeRecipient> existing = recipientRepository
            .findByEnvelopeIdAndEmail(envelopeId, email);

        if (existing.isPresent()) {
            log.warn("Recipient {} already exists for envelope {}", email, envelopeId);
            return existing.get();
        }

        EnvelopeRecipient recipient = new EnvelopeRecipient();
        recipient.setEnvelopeId(envelopeId);
        recipient.setName(name);
        recipient.setEmail(email);
        recipient.setRole(role != null ? role : "signer");
        recipient.setRoutingOrder(routingOrder != null ? routingOrder : 1);
        recipient.setStatus("pending");

        recipient = recipientRepository.save(recipient);
        log.info("Added recipient {} to envelope {}", email, envelopeId);

        return recipient;
    }

    /**
     * Send envelope
     */
    public Envelope sendEnvelope(String envelopeId) {
        Optional<Envelope> envOpt = envelopeRepository.findById(envelopeId);

        if (envOpt.isEmpty()) {
            throw new IllegalArgumentException("Envelope not found: " + envelopeId);
        }

        Envelope envelope = envOpt.get();

        // Validate envelope has recipients
        List<EnvelopeRecipient> recipients = recipientRepository
            .findByEnvelopeIdOrderByRoutingOrderAsc(envelopeId);

        if (recipients.isEmpty()) {
            throw new IllegalStateException("Cannot send envelope without recipients");
        }

        // Update envelope status
        envelope.setStatus("sent");
        envelope = envelopeRepository.save(envelope);

        // Mark first routing order recipients as delivered
        int firstOrder = recipients.get(0).getRoutingOrder();
        for (EnvelopeRecipient recipient : recipients) {
            if (recipient.getRoutingOrder() == firstOrder) {
                recipient.setStatus("delivered");
                recipient.setDeliveredAt(Instant.now());
                recipientRepository.save(recipient);

                // Create notification for recipient
                createNotification(recipient.getEmail(),
                    "New document to sign",
                    "You have a new document waiting for your signature from " + envelope.getName(),
                    "envelope_sent",
                    envelopeId);
            }
        }

        log.info("Sent envelope {} with {} recipients", envelopeId, recipients.size());

        return envelope;
    }

    /**
     * Update envelope status
     */
    public Envelope updateEnvelopeStatus(String envelopeId, String status) {
        Optional<Envelope> envOpt = envelopeRepository.findById(envelopeId);

        if (envOpt.isEmpty()) {
            throw new IllegalArgumentException("Envelope not found: " + envelopeId);
        }

        Envelope envelope = envOpt.get();
        String oldStatus = envelope.getStatus();
        envelope.setStatus(status);
        envelope = envelopeRepository.save(envelope);

        log.info("Updated envelope {} status from {} to {}", envelopeId, oldStatus, status);

        return envelope;
    }

    /**
     * Mark recipient as signed
     */
    public EnvelopeRecipient markRecipientSigned(String envelopeId, String email) {
        Optional<EnvelopeRecipient> recOpt = recipientRepository
            .findByEnvelopeIdAndEmail(envelopeId, email);

        if (recOpt.isEmpty()) {
            throw new IllegalArgumentException("Recipient not found");
        }

        EnvelopeRecipient recipient = recOpt.get();
        recipient.setStatus("completed");
        recipient.setSignedAt(Instant.now());
        recipient = recipientRepository.save(recipient);

        // Check if all recipients have signed
        checkAndCompleteEnvelope(envelopeId);

        log.info("Recipient {} signed envelope {}", email, envelopeId);

        return recipient;
    }

    /**
     * Check if envelope is complete and update status
     */
    private void checkAndCompleteEnvelope(String envelopeId) {
        List<EnvelopeRecipient> recipients = recipientRepository
            .findByEnvelopeIdOrderByRoutingOrderAsc(envelopeId);

        boolean allCompleted = recipients.stream()
            .filter(r -> "signer".equals(r.getRole()))
            .allMatch(r -> "completed".equals(r.getStatus()));

        if (allCompleted) {
            updateEnvelopeStatus(envelopeId, "completed");

            // Notify sender
            Optional<Envelope> envOpt = envelopeRepository.findById(envelopeId);
            if (envOpt.isPresent()) {
                Envelope envelope = envOpt.get();
                createNotification(envelope.getSenderUserId(),
                    "Document completed",
                    "All recipients have signed " + envelope.getName(),
                    "envelope_completed",
                    envelopeId);
            }
        }
    }

    /**
     * Get envelope by ID
     */
    public Optional<Envelope> getEnvelope(String id, Long accountId) {
        return envelopeRepository.findByIdAndAccountId(id, accountId);
    }

    /**
     * Get all envelopes for account
     */
    public List<Envelope> getAllEnvelopes(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        return envelopeRepository.findByAccountIdOrderByCreatedAtDesc(accountId);
    }

    /**
     * Get envelopes by status
     */
    public List<Envelope> getEnvelopesByStatus(Long accountId, String status) {
        if (accountId == null) {
            return List.of();
        }
        return envelopeRepository.findByAccountIdAndStatus(accountId, status);
    }

    /**
     * Get envelope statistics
     */
    public EnvelopeStats getEnvelopeStats(Long accountId) {
        if (accountId == null) {
            return new EnvelopeStats();
        }

        EnvelopeStats stats = new EnvelopeStats();
        stats.setTotal(envelopeRepository.countByAccountId(accountId));
        stats.setDraft(envelopeRepository.countByAccountIdAndStatus(accountId, "draft"));
        stats.setSent(envelopeRepository.countByAccountIdAndStatus(accountId, "sent"));
        stats.setDelivered(envelopeRepository.countByAccountIdAndStatus(accountId, "delivered"));
        stats.setCompleted(envelopeRepository.countByAccountIdAndStatus(accountId, "completed"));
        stats.setDeclined(envelopeRepository.countByAccountIdAndStatus(accountId, "declined"));
        stats.setVoided(envelopeRepository.countByAccountIdAndStatus(accountId, "voided"));

        return stats;
    }

    /**
     * Get recipients for envelope
     */
    public List<EnvelopeRecipient> getRecipients(String envelopeId) {
        return recipientRepository.findByEnvelopeIdOrderByRoutingOrderAsc(envelopeId);
    }

    /**
     * Search envelopes
     */
    public List<Envelope> searchEnvelopes(Long accountId, String query) {
        if (accountId == null || query == null || query.isBlank()) {
            return getAllEnvelopes(accountId);
        }
        return envelopeRepository.searchByNameOrSubject(accountId, query);
    }

    /**
     * Get action required envelopes
     */
    public List<Envelope> getActionRequired(Long accountId) {
        if (accountId == null) {
            return List.of();
        }
        return envelopeRepository.findActionRequired(accountId);
    }

    /**
     * Get waiting for others envelopes
     */
    public List<Envelope> getWaitingForOthers(Long accountId, String senderUserId) {
        if (accountId == null || senderUserId == null) {
            return List.of();
        }
        return envelopeRepository.findWaitingForOthers(accountId, senderUserId);
    }

    /**
     * Void envelope
     */
    public Envelope voidEnvelope(String envelopeId, String reason) {
        Optional<Envelope> envOpt = envelopeRepository.findById(envelopeId);

        if (envOpt.isEmpty()) {
            throw new IllegalArgumentException("Envelope not found: " + envelopeId);
        }

        Envelope envelope = envOpt.get();
        envelope.setStatus("voided");
        envelope = envelopeRepository.save(envelope);

        // Notify recipients
        List<EnvelopeRecipient> recipients = recipientRepository
            .findByEnvelopeIdOrderByRoutingOrderAsc(envelopeId);

        for (EnvelopeRecipient recipient : recipients) {
            createNotification(recipient.getEmail(),
                "Document voided",
                "The document " + envelope.getName() + " has been voided. Reason: " + reason,
                "envelope_voided",
                envelopeId);
        }

        log.info("Voided envelope {}: {}", envelopeId, reason);

        return envelope;
    }

    /**
     * Delete envelope (only drafts)
     */
    public boolean deleteEnvelope(String envelopeId, Long accountId) {
        Optional<Envelope> envOpt = envelopeRepository.findByIdAndAccountId(envelopeId, accountId);

        if (envOpt.isEmpty()) {
            return false;
        }

        Envelope envelope = envOpt.get();

        // Only allow deletion of drafts
        if (!"draft".equals(envelope.getStatus())) {
            throw new IllegalStateException("Can only delete draft envelopes");
        }

        // Delete recipients
        List<EnvelopeRecipient> recipients = recipientRepository
            .findByEnvelopeIdOrderByRoutingOrderAsc(envelopeId);
        recipientRepository.deleteAll(recipients);

        // Delete envelope
        envelopeRepository.delete(envelope);

        log.info("Deleted envelope {}", envelopeId);

        return true;
    }

    /**
     * Create notification
     */
    private void createNotification(String userId, String title, String message,
                                    String type, String envelopeId) {
        try {
            Notification notification = new Notification();
            notification.setUserId(userId);
            notification.setTitle(title);
            notification.setMessage(message);
            notification.setType(type);
            notification.setRelatedEnvelopeId(envelopeId);
            notification.setRead(false);

            notificationRepository.save(notification);
        } catch (Exception e) {
            log.error("Failed to create notification", e);
        }
    }

    /**
     * Envelope statistics DTO
     */
    public static class EnvelopeStats {
        private long total;
        private long draft;
        private long sent;
        private long delivered;
        private long completed;
        private long declined;
        private long voided;

        public long getTotal() { return total; }
        public void setTotal(long total) { this.total = total; }

        public long getDraft() { return draft; }
        public void setDraft(long draft) { this.draft = draft; }

        public long getSent() { return sent; }
        public void setSent(long sent) { this.sent = sent; }

        public long getDelivered() { return delivered; }
        public void setDelivered(long delivered) { this.delivered = delivered; }

        public long getCompleted() { return completed; }
        public void setCompleted(long completed) { this.completed = completed; }

        public long getDeclined() { return declined; }
        public void setDeclined(long declined) { this.declined = declined; }

        public long getVoided() { return voided; }
        public void setVoided(long voided) { this.voided = voided; }
    }
}
