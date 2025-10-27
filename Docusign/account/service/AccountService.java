package com.example.Docusign.account.service;

import java.security.Principal;
import java.util.List;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.Docusign.account.dto.AccountCreateRequest;
import com.example.Docusign.account.dto.AccountResponse;

@Service
@Transactional
public class AccountService {

    public AccountService() {}

    public AccountResponse createAccount(AccountCreateRequest request, Principal principal, Authentication authentication) {
        // No-op stub to keep controllers functioning during refactor
        return new AccountResponse();
    }

    public List<AccountResponse> listMyAccounts(Authentication authentication) {
        return java.util.List.of();
    }

    public AccountResponse getAccountDetails(Long accountId, Authentication authentication) {
        throw new IllegalArgumentException("Account API disabled");
    }

    public void removeMember(Long accountId, String memberKeycloakId, Authentication authentication) {
        throw new AccessDeniedException("Account API disabled");
    }

    // note: Individual account user records are now in `individual_accounts` table
    // and managed separately from workspace `accounts`.
}
