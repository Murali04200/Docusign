package com.example.Docusign.account.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.Docusign.account.dto.AccountCreateRequest;
import com.example.Docusign.account.dto.AccountResponse;
import com.example.Docusign.account.service.AccountService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/accounts")
@Validated
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody AccountCreateRequest request, Principal principal, Authentication authentication) {
        AccountResponse response = accountService.createAccount(request, principal, authentication);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts(Authentication authentication) {
        return ResponseEntity.ok(accountService.listMyAccounts(authentication));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable Long accountId, Authentication authentication) {
        return ResponseEntity.ok(accountService.getAccountDetails(accountId, authentication));
    }

    @PostMapping("/{accountId}/invite/{inviteeKeycloakId}")
    @PreAuthorize("hasRole('OWNER') or hasRole('ADMIN')")
    public ResponseEntity<Void> inviteMember(
            @PathVariable Long accountId,
            @PathVariable String inviteeKeycloakId,
            Authentication authentication) {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
    }
}
