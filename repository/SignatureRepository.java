package com.example.Docusign.repository;

import com.example.Docusign.account.model.IndividualAccount;
import com.example.Docusign.model.Signature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SignatureRepository extends JpaRepository<Signature, Long> {

    /**
     * Find all signatures for a specific account
     */
    List<Signature> findByAccountOrderByCreatedAtDesc(IndividualAccount account);

    /**
     * Find all signatures by account ID
     */
    @Query("SELECT s FROM Signature s WHERE s.account.id = :accountId ORDER BY s.createdAt DESC")
    List<Signature> findByAccountId(@Param("accountId") Long accountId);

    /**
     * Find default signature for an account
     */
    Optional<Signature> findByAccountAndIsDefaultTrue(IndividualAccount account);

    /**
     * Find default signature by account ID
     */
    @Query("SELECT s FROM Signature s WHERE s.account.id = :accountId AND s.isDefault = true")
    Optional<Signature> findDefaultSignatureByAccountId(@Param("accountId") Long accountId);

    /**
     * Count signatures for an account
     */
    long countByAccount(IndividualAccount account);

    /**
     * Check if signature exists for account
     */
    boolean existsByIdAndAccount(Long id, IndividualAccount account);

    /**
     * Set all signatures to non-default for an account
     */
    @Modifying
    @Query("UPDATE Signature s SET s.isDefault = false WHERE s.account.id = :accountId")
    void clearDefaultSignatures(@Param("accountId") Long accountId);

    /**
     * Delete signature by ID and account (security check)
     */
    void deleteByIdAndAccount(Long id, IndividualAccount account);

    /**
     * Find signature by ID and account (security check)
     */
    Optional<Signature> findByIdAndAccount(Long id, IndividualAccount account);
}
