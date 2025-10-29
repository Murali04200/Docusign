package com.example.Docusign.repository;

import com.example.Docusign.model.EnvelopeRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EnvelopeRecipientRepository extends JpaRepository<EnvelopeRecipient, Long> {

    // Find all recipients for an envelope
    List<EnvelopeRecipient> findByEnvelopeIdOrderByRoutingOrderAsc(String envelopeId);

    // Find by envelope and status
    List<EnvelopeRecipient> findByEnvelopeIdAndStatus(String envelopeId, String status);

    // Count by status
    long countByEnvelopeIdAndStatus(String envelopeId, String status);

    // Find by email
    List<EnvelopeRecipient> findByEmailOrderByCreatedAtDesc(String email);

    // Find recipient by envelope and email
    Optional<EnvelopeRecipient> findByEnvelopeIdAndEmail(String envelopeId, String email);

    // Check if recipient exists
    boolean existsByEnvelopeIdAndEmail(String envelopeId, String email);

    // Find pending recipients
    @Query("SELECT r FROM EnvelopeRecipient r WHERE r.envelopeId = :envelopeId AND r.status = 'pending'")
    List<EnvelopeRecipient> findPendingRecipients(@Param("envelopeId") String envelopeId);

    // Find next recipient in routing order
    @Query("SELECT r FROM EnvelopeRecipient r WHERE r.envelopeId = :envelopeId AND " +
           "r.status = 'pending' ORDER BY r.routingOrder ASC")
    List<EnvelopeRecipient> findNextRecipients(@Param("envelopeId") String envelopeId);
}
