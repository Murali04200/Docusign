package com.example.Docusign.account.dto;

import com.example.Docusign.account.model.AccountType;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AccountCreateRequest {

    @NotBlank
    @Size(min = 3, max = 100)
    private String name;

    @NotNull
    private AccountType type;

    @Email
    private String ownerEmailOverride;

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

    public String getOwnerEmailOverride() {
        return ownerEmailOverride;
    }

    public void setOwnerEmailOverride(String ownerEmailOverride) {
        this.ownerEmailOverride = ownerEmailOverride;
    }
}
