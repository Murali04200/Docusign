package com.example.Docusign.account.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.Docusign.account.model.IndividualAccount;
import java.util.Optional;

public interface IndividualAccountRepository extends JpaRepository<IndividualAccount, Long> {
    Optional<IndividualAccount> findByEmail(String email);
}
