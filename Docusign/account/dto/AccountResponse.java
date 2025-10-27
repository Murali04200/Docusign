package com.example.Docusign.account.dto;

import java.time.Instant;
import java.util.List;

import com.example.Docusign.account.model.AccountType;
import com.example.Docusign.account.model.MemberRole;

public class AccountResponse {

    private Long id;
    private String name;
    private AccountType type;
    private String ownerDisplayName;
    private String ownerEmail;
    private Instant createdAt;
    private Instant updatedAt;
    private List<MemberSummary> members;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AccountType getType() {
        return type;
    }

    public void setType(AccountType type) {
        this.type = type;
    }

    public String getOwnerDisplayName() {
        return ownerDisplayName;
    }

    public void setOwnerDisplayName(String ownerDisplayName) {
        this.ownerDisplayName = ownerDisplayName;
    }

    public String getOwnerEmail() {
        return ownerEmail;
    }

    public void setOwnerEmail(String ownerEmail) {
        this.ownerEmail = ownerEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<MemberSummary> getMembers() {
        return members;
    }

    public void setMembers(List<MemberSummary> members) {
        this.members = members;
    }

    public static class MemberSummary {
        private String keycloakUserId;
        private String email;
        private String displayName;
        private MemberRole role;
        private Instant joinedAt;

        public String getKeycloakUserId() {
            return keycloakUserId;
        }

        public void setKeycloakUserId(String keycloakUserId) {
            this.keycloakUserId = keycloakUserId;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public MemberRole getRole() {
            return role;
        }

        public void setRole(MemberRole role) {
            this.role = role;
        }

        public Instant getJoinedAt() {
            return joinedAt;
        }

        public void setJoinedAt(Instant joinedAt) {
            this.joinedAt = joinedAt;
        }
    }
}
