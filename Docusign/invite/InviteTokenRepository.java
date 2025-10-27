package com.example.Docusign.invite;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface InviteTokenRepository extends JpaRepository<InviteToken, String> {
    Optional<InviteToken> findByToken(String token);
    java.util.List<InviteToken> findAllByInviterUserIdOrderByCreatedAtDesc(String inviterUserId);
}
