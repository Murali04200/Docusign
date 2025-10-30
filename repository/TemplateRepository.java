package com.example.Docusign.repository;

import com.example.Docusign.model.Template;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TemplateRepository extends JpaRepository<Template, String> {

    List<Template> findByAccountIdOrderByCreatedAtDesc(Long accountId);

    Optional<Template> findByIdAndAccountId(String id, Long accountId);

    List<Template> findByAccountIdAndNameContainingIgnoreCase(Long accountId, String name);

    long countByAccountId(Long accountId);
}
