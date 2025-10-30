package com.example.Docusign.repository;

import com.example.Docusign.model.Envelope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface EnvelopeRepository extends JpaRepository<Envelope, String> {

    // Find by account
    List<Envelope> findByAccountIdOrderByCreatedAtDesc(Long accountId);
    Page<Envelope> findByAccountId(Long accountId, Pageable pageable);

    // Find by account and status
    List<Envelope> findByAccountIdAndStatus(Long accountId, String status);
    Page<Envelope> findByAccountIdAndStatus(Long accountId, String status, Pageable pageable);

    // Count by status
    long countByAccountIdAndStatus(Long accountId, String status);
    long countByAccountId(Long accountId);

    // Find by sender
    List<Envelope> findBySenderUserIdOrderByCreatedAtDesc(String senderUserId);

    // Find by ID and account
    Optional<Envelope> findByIdAndAccountId(String id, Long accountId);

    // Search by name or subject
    @Query("SELECT e FROM Envelope e WHERE e.accountId = :accountId AND " +
           "(LOWER(e.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(e.subject) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Envelope> searchByNameOrSubject(@Param("accountId") Long accountId,
                                         @Param("query") String query);

    // Recent envelopes
    List<Envelope> findTop10ByAccountIdOrderByCreatedAtDesc(Long accountId);

    // Find envelopes created after a certain time
    List<Envelope> findByAccountIdAndCreatedAtAfter(Long accountId, Instant after);

    // Action required (sent but not completed)
    @Query("SELECT e FROM Envelope e WHERE e.accountId = :accountId AND " +
           "e.status IN ('sent', 'delivered') ORDER BY e.createdAt DESC")
    List<Envelope> findActionRequired(@Param("accountId") Long accountId);

    // Waiting for others
    @Query("SELECT e FROM Envelope e WHERE e.accountId = :accountId AND " +
           "e.senderUserId = :senderUserId AND " +
           "e.status IN ('sent', 'delivered') ORDER BY e.createdAt DESC")
    List<Envelope> findWaitingForOthers(@Param("accountId") Long accountId,
                                         @Param("senderUserId") String senderUserId);

    // Expiring soon (custom logic would be needed based on expiration field)
    @Query("SELECT e FROM Envelope e WHERE e.accountId = :accountId AND " +
           "e.status = 'sent' ORDER BY e.createdAt ASC")
    List<Envelope> findExpiringSoon(@Param("accountId") Long accountId, Pageable pageable);
}
