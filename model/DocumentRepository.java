package com.example.Docusign.model;

import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentRepository extends JpaRepository<Document, String> {
    long countByAccountIdAndStatus(Long accountId, String status);

    List<Document> findByAccountIdOrderByLastModifiedDesc(Long accountId, Pageable pageable);
}
